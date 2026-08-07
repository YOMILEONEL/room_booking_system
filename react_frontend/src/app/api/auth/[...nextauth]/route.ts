import NextAuth from "next-auth";
import CredentialsProvider from "next-auth/providers/credentials";
import { BACKEND_BASE_URL } from "../../http";

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

        const res = await fetch(`${BACKEND_BASE_URL}/api/login`, {
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

        // Erwartete AuthResponse: { id, username, role, accessToken }
        if (!data || !data.id || !data.accessToken) {
          return null;
        }

        return {
          id: String(data.id),
          name: data.username,
          role: data.role,
          accessToken: data.accessToken,
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
        token.userId = user.id;
        token.accessToken = user.accessToken;
      }
      return token;
    },
    async session({ session, token }) {
      if (session.user) {
        session.user.role = token.role;
        session.user.id = token.userId;
      }
      session.accessToken = token.accessToken;
      return session;
    },
  },

  secret: process.env.NEXTAUTH_SECRET ?? process.env.AUTH_SECRET,
});

export { handler as GET, handler as POST };
