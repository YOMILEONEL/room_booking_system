import { apiFetch } from "./apiClient";

const BASE = "/discount-code";

export type DiscountType = "PERCENT" | "ABSOLUTE";

export type DiscountCode = {
  id: string;
  code: string;
  type: DiscountType;
  value: number;
  validFrom: string;
  validUntil: string;
  active: boolean;
};

export type CreateDiscountCodeRequest = {
  code: string;
  type: DiscountType;
  value: number;
  validFrom: string;
  validUntil: string;
  active: boolean;
};

export async function fetchDiscountCodes(): Promise<DiscountCode[]> {
  return apiFetch<DiscountCode[]>(BASE);
}

export async function createDiscountCode(payload: CreateDiscountCodeRequest): Promise<DiscountCode> {
  return apiFetch<DiscountCode>(BASE, { method: "POST", body: payload });
}

export async function deleteDiscountCode(id: string): Promise<void> {
  await apiFetch<void>(`${BASE}/${id}`, { method: "DELETE" });
}
