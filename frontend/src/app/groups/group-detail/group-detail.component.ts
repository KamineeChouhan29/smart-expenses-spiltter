import { Component, OnInit, signal, computed } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { GroupService } from '../../core/services/group.service';
import { ExpenseService } from '../../core/services/expense.service';
import { AuthService } from '../../core/services/auth.service';
import { NavbarComponent } from '../../shared/navbar/navbar.component';
import { AddExpenseComponent } from '../../expenses/add-expense/add-expense.component';
import { BalanceEntry, Expense, GroupMember, ExpenseRequest } from '../../core/models/models';

@Component({
  selector: 'app-group-detail',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterLink, NavbarComponent, AddExpenseComponent],
  templateUrl: './group-detail.component.html'
})
export class GroupDetailComponent implements OnInit {
  groupId!: number;
  groupName = signal('');
  members   = signal<GroupMember[]>([]);
  expenses  = computed(() => this.expenseService.expenses());
  balances  = signal<BalanceEntry[]>([]);

  loading         = signal(true);
  loadingBalances = signal(false);
  balancesLoaded  = signal(false);

  // Add member
  newMemberUsername = signal('');
  addMemberError    = signal('');
  addMemberLoading  = signal(false);
  showMemberPanel   = signal(false);

  // Add expense modal
  showExpenseModal = signal(false);

  currentUser = computed(() => this.authService.currentUser());

  constructor(
    private route: ActivatedRoute,
    private router: Router,
    private groupService: GroupService,
    private expenseService: ExpenseService,
    private authService: AuthService
  ) {}

  ngOnInit(): void {
    this.groupId = Number(this.route.snapshot.paramMap.get('id'));
    this.loadAll();
  }

  private loadAll(): void {
    this.loading.set(true);
    // Load members
    this.groupService.getMembers(this.groupId).subscribe({
      next: (m) => {
        this.members.set(m);
        this.loading.set(false);
      },
      error: () => this.loading.set(false)
    });
    // Load expenses
    this.expenseService.getGroupExpenses(this.groupId).subscribe();
  }

  loadBalances(): void {
    this.loadingBalances.set(true);
    this.balancesLoaded.set(true);
    this.groupService.getBalances(this.groupId).subscribe({
      next: (b) => { this.balances.set(b); this.loadingBalances.set(false); },
      error: ()  => this.loadingBalances.set(false)
    });
  }

  addMember(): void {
    const username = this.newMemberUsername().trim();
    if (!username) { this.addMemberError.set('Enter a username.'); return; }
    this.addMemberLoading.set(true);
    this.addMemberError.set('');
    this.groupService.addMember(this.groupId, username).subscribe({
      next: () => {
        this.addMemberLoading.set(false);
        this.newMemberUsername.set('');
        this.showMemberPanel.set(false);
        // Refresh members
        this.groupService.getMembers(this.groupId).subscribe(m => this.members.set(m));
      },
      error: (err) => {
        this.addMemberError.set(err.error?.error || 'Could not add member.');
        this.addMemberLoading.set(false);
      }
    });
  }

  onExpenseAdded(): void {
    this.showExpenseModal.set(false);
    // Refresh expenses and balances
    this.expenseService.getGroupExpenses(this.groupId).subscribe();
    if (this.balancesLoaded()) {
      this.loadBalances();
    }
  }

  getCategoryClass(category: string): string {
    const map: Record<string, string> = {
      'Food': 'badge-food', 'Travel': 'badge-travel',
      'Rent': 'badge-rent', 'Shopping': 'badge-shopping', 'Other': 'badge-other'
    };
    return map[category] || 'badge-other';
  }

  getCategoryEmoji(category: string): string {
    const map: Record<string, string> = {
      'Food': '🍔', 'Travel': '✈️', 'Rent': '🏠', 'Shopping': '🛍️', 'Other': '💰'
    };
    return map[category] || '💰';
  }

  getInitial(name: string): string {
    return name?.charAt(0)?.toUpperCase() || '?';
  }

  formatAmount(n: number): string {
    return '₹' + n.toLocaleString('en-IN', { minimumFractionDigits: 2, maximumFractionDigits: 2 });
  }

  toggleMemberPanel(): void { this.showMemberPanel.update(v => !v); }
  openExpenseModal(): void  { this.showExpenseModal.set(true); }
  closeExpenseModal(): void { this.showExpenseModal.set(false); }

  goBack(): void { this.router.navigate(['/dashboard']); }
}
