import NextAuth from "next-auth";
import type { JWT } from "next-auth/jwt";
import CredentialsProvider from "next-auth/providers/credentials";
import { BACKEND_INTERNAL_URL } from "../../http";

// Liest die "exp"-Claim (Sekunden seit Epoch) aus einem JWT, ohne die Signatur zu prüfen -
// die Prüfung übernimmt ausschließlich das Backend. Fällt der Payload nicht auslesbar,
// nehmen wir eine konservative Standard-TTL an.
function decodeAccessTokenExpiryMs(accessToken: string): number {
  const fallback = Date.now() + 55 * 60 * 1000;
  try {
    const payload = accessToken.split(".")[1];
    const json = Buffer.from(payload, "base64url").toString("utf-8");
    const decoded = JSON.parse(json) as { exp?: number };
    return typeof decoded.exp === "number" ? decoded.exp * 1000 : fallback;
  } catch {
    return fallback;
  }
}

async function refreshAccessToken(token: JWT): Promise<JWT> {
  try {
    const res = await fetch(`${BACKEND_INTERNAL_URL}/api/refresh`, {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({ refreshToken: token.refreshToken }),
    });

    if (!res.ok) {
      throw new Error(`Refresh failed with status ${res.status}`);
    }

    const data = (await res.json()) as { accessToken: string };

    return {
      ...token,
      accessToken: data.accessToken,
      accessTokenExpires: decodeAccessTokenExpiryMs(data.accessToken),
      error: undefined,
    };
  } catch (err) {
    console.error("Failed to refresh access token:", err);
    // Access-Token bleibt wie er ist - der nächste Backend-Call schlägt dann mit 401/403
    // fehl, was aus Nutzersicht "bitte neu einloggen" bedeutet.
    return { ...token, error: "RefreshAccessTokenError" };
  }
}

const handler = NextAuth({
  providers: [
    CredentialsProvider({
      name: "Credentials",
      credentials: {
        username: { label: "Username", type: "text" },
        password: { label: "Password", type: "password" },
      },
      async authorize(credentials) {
        if (!credentials?.username || !credentials?.password) {
          return null;
        }

        const res = await fetch(`${BACKEND_INTERNAL_URL}/api/login`, {
          method: "POST",
          headers: { "Content-Type": "application/json" },
          body: JSON.stringify({
            username: credentials.username,
            password: credentials.password,
          }),
        });

        // Dein Spring-Backend gibt bei falschen Credentials 401 zurück
        if (!res.ok) {
          return null;
        }

        const data = await res.json();

        // Erwartete AuthResponse: { id, username, role, accessToken, refreshToken }
        if (!data || !data.id || !data.accessToken || !data.refreshToken) {
          return null;
        }

        return {
          id: String(data.id),
          name: data.username,
          displayName: data.displayName,
          role: data.role,
          customerType: data.customerType,
          accessToken: data.accessToken,
          refreshToken: data.refreshToken,
        };
      },
    }),
  ],

  pages: {
    signIn: "/login",
  },

  // KEIN redirect-Callback hier → weniger URL-Magie, weniger Fehler
  callbacks: {
    async jwt({ token, user }) {
      if (user) {
        token.role = user.role;
        token.customerType = user.customerType;
        token.displayName = user.displayName;
        token.userId = user.id;
        token.accessToken = user.accessToken;
        token.refreshToken = user.refreshToken;
        token.accessTokenExpires = decodeAccessTokenExpiryMs(user.accessToken);
        return token;
      }

      // Ein vorheriger Refresh-Versuch ist bereits fehlgeschlagen (z. B. Refresh-Token
      // wurde serverseitig ungültig/gelöscht) - nicht bei jeder einzelnen Anfrage erneut
      // versuchen, das würde das Backend nur mit Dauerversuchen fluten. Der Nutzer muss
      // sich neu einloggen, um einen frischen Token zu bekommen.
      if (token.error === "RefreshAccessTokenError") {
        return token;
      }

      // Access-Token noch gültig (mit 30s Puffer) - nichts zu tun.
      if (token.accessTokenExpires && Date.now() < token.accessTokenExpires - 30_000) {
        return token;
      }

      return refreshAccessToken(token);
    },
    async session({ session, token }) {
      if (session.user) {
        session.user.role = token.role;
        session.user.customerType = token.customerType;
        session.user.displayName = token.displayName;
        session.user.id = token.userId;
      }
      session.accessToken = token.accessToken;
      session.error = token.error;
      return session;
    },
  },

  secret: process.env.NEXTAUTH_SECRET ?? process.env.AUTH_SECRET,
});

export { handler as GET, handler as POST };
