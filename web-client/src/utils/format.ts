export function formatCents(cents: number): string {
  return `$${(cents / 100).toFixed(2)}`;
}

export function formatCurrency(dollars: number): string {
  return `$${dollars.toFixed(2)}`;
}

export function dollarsToCents(dollars: string | number): number {
  const amount = typeof dollars === 'string' ? parseFloat(dollars) : dollars;
  return Math.round(amount * 100);
}
