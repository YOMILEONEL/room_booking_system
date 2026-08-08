import "next-auth";
import "next-auth/jwt";

declare module "next-auth" {
  interface Session {
    accessToken?: string;
    error?: string;
    user?: {
      id?: string;
      name?: string | null;
      displayName?: string;
      role?: string;
      customerType?: string;
    };
  }

  interface User {
    id: string;
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
  }
}
