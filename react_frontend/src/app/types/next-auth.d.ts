import "next-auth";
import "next-auth/jwt";

declare module "next-auth" {
  interface Session {
    accessToken?: string;
    error?: string;
    // Set once at login (see lib/auth.ts's jwt() callback) - the assistant session created for
    // this login, so /assistant can default to it while still letting the person pick an older
    // one instead.
    freshAssistantSessionId?: string;
    user?: {
      id?: string;
      email?: string | null;
      displayName?: string;
      role?: string;
      customerType?: string;
    };
  }

  interface User {
    id: string;
    email?: string | null;
    displayName?: string;
    role?: string;
    customerType?: string;
    accessToken: string;
    refreshToken: string;
  }
}

declare module "next-auth/jwt" {
  interface JWT {
    accessToken?: string;
    refreshToken?: string;
    accessTokenExpires?: number;
    displayName?: string;
    role?: string;
    customerType?: string;
    userId?: string;
    error?: string;
    freshAssistantSessionId?: string;
  }
}
