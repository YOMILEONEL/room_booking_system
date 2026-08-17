import type { NextAuthOptions } from "next-auth";
import type { JWT } from "next-auth/jwt";
import CredentialsProvider from "next-auth/providers/credentials";
import { BACKEND_INTERNAL_URL } from "../api/http";

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

// Lives outside app/api/auth/[...nextauth]/route.ts on purpose: Next.js's App Router only
// allows a fixed set of exports (GET/POST/config/...) from a route.ts file and fails the
// build on anything else, so authOptions can't be exported from there directly. Other
// server-side code - e.g. the /api/assistant route - imports it from here to call
// getServerSession(authOptions) without a client round-trip.
export const authOptions: NextAuthOptions = {
  providers: [
    CredentialsProvider({
      name: "Credentials",
      credentials: {
        email: { label: "E-Mail", type: "email" },
        password: { label: "Password", type: "password" },
      },
      async authorize(credentials) {
        if (!credentials?.email || !credentials?.password) {
          return null;
        }

        const res = await fetch(`${BACKEND_INTERNAL_URL}/api/login`, {
          method: "POST",
          headers: { "Content-Type": "application/json" },
          body: JSON.stringify({
            email: credentials.email,
            password: credentials.password,
          }),
        });

        // Dein Spring-Backend gibt bei falschen Credentials 401 zurück
        if (!res.ok) {
          return null;
        }

        const data = await res.json();

        // Erwartete AuthResponse: { id, email, role, accessToken, refreshToken }
        if (!data || !data.id || !data.accessToken || !data.refreshToken) {
          return null;
        }

        return {
          id: String(data.id),
          email: data.email,
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
    async jwt({ token, user, trigger, session }) {
      if (user) {
        token.email = user.email;
        token.role = user.role;
        token.customerType = user.customerType;
        token.displayName = user.displayName;
        token.userId = user.id;
        token.accessToken = user.accessToken;
        token.refreshToken = user.refreshToken;
        token.accessTokenExpires = decodeAccessTokenExpiryMs(user.accessToken);
        return token;
      }

      // Ausgelöst durch useSession().update(...) nach einer Profiländerung. Das Access-Token
      // trägt die (alte) E-Mail als "sub" - ohne erzwungenen Refresh würde jeder Backend-Call
      // bis zu ~60 Minuten mit 403 fehlschlagen, obwohl die Änderung schon gespeichert ist
      // (siehe docs/code-review.md, 6.1). refreshAccessToken holt sich ein frisches Token vom
      // Backend, das dessen *aktuelle* E-Mail als "sub" trägt (RegistrationLoginController.refresh
      // liest sie über die User-Relation, nicht über das alte Lookup).
      if (trigger === "update") {
        const refreshed = await refreshAccessToken(token);
        if (typeof session?.email === "string") {
          refreshed.email = session.email;
        }
        // Same reasoning as email above: the JWT itself never carries displayName, only the
        // token this update() call was seeded with does - without this, editing your name in
        // the profile would save fine server-side but the NavBar would keep showing the old
        // value (or the email fallback) until the token naturally expired.
        if (typeof session?.displayName === "string") {
          refreshed.displayName = session.displayName;
        }
        return refreshed;
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
        // NextAuth seedet session.user.email selbst aus dem *alten*, noch nicht durch den
        // jwt()-Callback aktualisierten Token - ohne diese Zeile bliebe die E-Mail nach einem
        // erzwungenen Refresh (trigger: "update", siehe jwt() oben) im UI auf dem alten Stand,
        // obwohl accessToken/JWT-Subject schon korrekt sind.
        session.user.email = token.email;
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
};
