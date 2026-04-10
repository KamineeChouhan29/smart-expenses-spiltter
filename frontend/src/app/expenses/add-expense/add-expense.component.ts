import { Component, EventEmitter, Input, OnInit, Output, signal, computed } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ExpenseService } from '../../core/services/expense.service';
import { AuthService } from '../../core/services/auth.service';
import { GroupMember, ExpenseRequest, Expense } from '../../core/models/models';

interface CustomSplitEntry {
  userId: number;
  username: string;
  amount: number;
  percentage: number;
}

@Component({
  selector: 'app-add-expense',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './add-expense.component.html'
})
export class AddExpenseComponent implements OnInit {
  Math = Math; // expose to template
  @Input() groupId!: number;
  @Input() members: GroupMember[] = [];
  @Output() expenseAdded = new EventEmitter<void>();
  @Output() cancelled    = new EventEmitter<void>();

  @Input() editExpense?: Expense;

  currentUser = computed(() => this.authService.currentUser());

  description   = '';
  amount        = 0;
  paidByUserId  = 0;
  splitType: 'EQUAL' | 'CUSTOM' = 'EQUAL';
  splitMode: 'AMOUNT' | 'PERCENTAGE' = 'AMOUNT';
  customSplits: CustomSplitEntry[] = [];

  loading = signal(false);
  error   = signal('');

  constructor(
    private expenseService: ExpenseService,
    private authService: AuthService
  ) {}

  ngOnInit(): void {
    if (this.editExpense) {
      this.description = this.editExpense.description;
      this.amount = this.editExpense.amount;
      const payer = this.members.find(m => m.username === this.editExpense!.paidBy);
      this.paidByUserId = payer ? payer.id : (this.members[0]?.id ?? 0);
      this.splitType = this.editExpense.splitType as 'EQUAL' | 'CUSTOM';
      
      this.initCustomSplits(); 
      if (this.splitType === 'CUSTOM') {
         this.splitMode = 'AMOUNT';
         if (this.editExpense.splits) {
             this.customSplits = this.members.map(m => {
                 const split = this.editExpense!.splits!.find(s => s.userId === m.id);
                 return {
                     userId: m.id,
                     username: m.username,
                     amount: split ? split.amountOwed : 0,
                     percentage: 0
                 };
             });
         }
      }
    } else {
      const uid = this.currentUser()?.id;
      if (uid) {
        const me = this.members.find(m => m.id === uid);
        this.paidByUserId = me ? me.id : (this.members[0]?.id ?? 0);
      }
      this.initCustomSplits();
    }
  }

  initCustomSplits(): void {
    const each = this.amount
      ? parseFloat((this.amount / this.members.length).toFixed(2))
      : 0;
    const equalPct = this.members.length
      ? parseFloat((100 / this.members.length).toFixed(2))
      : 0;

    this.customSplits = this.members.map(m => ({
      userId: m.id,
      username: m.username,
      amount: each,
      percentage: equalPct
    }));
  }

  onAmountChange(): void {
    if (this.splitType === 'CUSTOM') this.initCustomSplits();
  }

  onSplitTypeChange(): void {
    if (this.splitType === 'CUSTOM') this.initCustomSplits();
  }

  getTotalCustom(): number {
    if (this.splitMode === 'PERCENTAGE') {
      return this.customSplits.reduce((s, e) => s + (e.percentage || 0), 0);
    }
    return this.customSplits.reduce((s, e) => s + (e.amount || 0), 0);
  }

  isValid(): boolean {
    if (!this.description.trim() || this.amount <= 0 || !this.paidByUserId) return false;
    if (this.splitType === 'CUSTOM') {
      const total = this.getTotalCustom();
      if (this.splitMode === 'PERCENTAGE') return Math.abs(total - 100) < 0.01;
      return Math.abs(total - this.amount) < 0.01;
    }
    return true;
  }

  submit(): void {
    this.error.set('');
    if (!this.isValid()) {
      if (this.splitType === 'CUSTOM') {
        const total = this.getTotalCustom();
        const expected = this.splitMode === 'PERCENTAGE' ? 100 : this.amount;
        this.error.set(`Split totals ₹${total.toFixed(2)} but must equal ₹${expected.toFixed(2)}`);
      } else {
        this.error.set('Please fill in all required fields correctly.');
      }
      return;
    }

    this.loading.set(true);

    const req: ExpenseRequest = {
      groupId: this.groupId,
      description: this.description.trim(),
      amount: this.amount,
      paidByUserId: this.paidByUserId,
      splitType: this.splitType
    };

    if (this.splitType === 'CUSTOM') {
      req.splitMode = this.splitMode;
      req.splits = this.customSplits.map(e => ({
        userId: e.userId,
        amount: this.splitMode === 'AMOUNT' ? e.amount : undefined,
        percentage: this.splitMode === 'PERCENTAGE' ? e.percentage : undefined
      }));
    }

    const obs$ = this.editExpense 
      ? this.expenseService.updateExpense(this.editExpense.id!, req)
      : this.expenseService.addExpense(req);

    obs$.subscribe({
      next: () => {
        this.loading.set(false);
        this.expenseAdded.emit();
      },
      error: (err) => {
        this.error.set(err.error?.error || 'Failed to process expense.');
        this.loading.set(false);
      }
    });
  }
}
