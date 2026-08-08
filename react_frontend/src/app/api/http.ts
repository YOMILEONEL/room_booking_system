// Used by the browser (client components) - reachable via the published Docker port
// or plain localhost in non-Docker dev.
export const BACKEND_BASE_URL =
  process.env.NEXT_PUBLIC_BACKEND_URL ?? "http://localhost:8080";

// Used by server-side code running inside the Next.js server itself (NextAuth's
// authorize/jwt callbacks). Inside Docker, "localhost" from the frontend container
// does NOT reach the backend container - it needs the Docker service DNS name
// instead (set via BACKEND_INTERNAL_URL in docker-compose.yml). Falls back to the
// public URL for local (non-Docker) dev, where both point at the same place.
export const BACKEND_INTERNAL_URL =
  process.env.BACKEND_INTERNAL_URL ?? BACKEND_BASE_URL;
