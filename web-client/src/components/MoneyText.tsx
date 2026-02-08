import { Typography } from '@mui/material';
import type { TypographyProps } from '@mui/material';
import { formatCents } from '../utils/format';

interface MoneyTextProps extends Omit<TypographyProps, 'color'> {
  cents: number;
  showSign?: boolean;
}

export function MoneyText({ cents, showSign = false, ...props }: MoneyTextProps) {
  const isPositive = cents > 0;
  const isNegative = cents < 0;

  let color: string | undefined;
  if (isPositive) {
    color = 'success.main';
  } else if (isNegative) {
    color = 'error.main';
  }

  const displayValue = showSign && isPositive ? `+${formatCents(cents)}` : formatCents(Math.abs(cents));

  return (
    <Typography {...props} color={color}>
      {displayValue}
    </Typography>
  );
}
