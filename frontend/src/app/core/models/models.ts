export interface User {
  id: number;
  username: string;
  email: string;
}

export interface JwtResponse {
  token: string;
  id: number;
  username: string;
  email: string;
}

export interface Group {
  id: number;
  name: string;
  createdBy: string;
  createdAt: string;
}

export interface GroupMember {
  id: number;
  username: string;
  email: string;
}

export interface Expense {
  id: number;
  description: string;
  amount: number;
  paidBy: string;
  category: string;
  splitType: 'EQUAL' | 'CUSTOM';
  createdAt: string;
  splits: SplitDetail[];
}

export interface SplitDetail {
  userId: number;
  username: string;
  amountOwed: number;
}

export interface BalanceEntry {
  debtor: string;
  creditor: string;
  amount: number;
}

export interface ExpenseRequest {
  groupId: number;
  description: string;
  amount: number;
  paidByUserId: number;
  splitType: 'EQUAL' | 'CUSTOM';
  splitMode?: 'AMOUNT' | 'PERCENTAGE';
  splits?: { userId: number; amount?: number; percentage?: number }[];
}
