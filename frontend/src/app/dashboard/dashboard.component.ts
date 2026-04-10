import { Component, OnInit, signal, computed } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { AuthService } from '../core/services/auth.service';
import { GroupService } from '../core/services/group.service';
import { ExpenseService } from '../core/services/expense.service';
import { NavbarComponent } from '../shared/navbar/navbar.component';
import { Group } from '../core/models/models';

@Component({
  selector: 'app-dashboard',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterLink, NavbarComponent],
  templateUrl: './dashboard.component.html'
})
export class DashboardComponent implements OnInit {
  user      = computed(() => this.authService.currentUser());
  groups    = computed(() => this.groupService.groups());

  insights      = signal<string[]>([]);
  loadingGroups = signal(true);
  loadingInsights = signal(false);
  insightsExpanded = signal(false);

  // Create group modal
  showCreateModal = signal(false);
  newGroupName    = signal('');
  createError     = signal('');
  createLoading   = signal(false);

  get greeting(): string {
    const h = new Date().getHours();
    if (h < 12) return 'Good morning';
    if (h < 18) return 'Good afternoon';
    return 'Good evening';
  }

  constructor(
    private authService: AuthService,
    private groupService: GroupService,
    private expenseService: ExpenseService,
    private router: Router
  ) {}

  ngOnInit(): void {
    this.groupService.loadGroups().subscribe({
      next: () => this.loadingGroups.set(false),
      error: () => this.loadingGroups.set(false)
    });
  }

  loadInsights(): void {
    if (this.insights().length > 0) {
      this.insightsExpanded.update(v => !v);
      return;
    }
    this.loadingInsights.set(true);
    this.insightsExpanded.set(true);
    this.expenseService.getInsights().subscribe({
      next: (data) => { this.insights.set(data); this.loadingInsights.set(false); },
      error: ()     => { this.insights.set(['Could not load insights. Please try again.']); this.loadingInsights.set(false); }
    });
  }

  openCreateModal(): void {
    this.showCreateModal.set(true);
    this.newGroupName.set('');
    this.createError.set('');
  }

  closeCreateModal(): void {
    this.showCreateModal.set(false);
  }

  createGroup(): void {
    const name = this.newGroupName().trim();
    if (!name) { this.createError.set('Group name is required.'); return; }
    this.createLoading.set(true);
    this.groupService.createGroup(name).subscribe({
      next: () => { this.createLoading.set(false); this.closeCreateModal(); },
      error: (err) => {
        this.createError.set(err.error?.error || 'Could not create group.');
        this.createLoading.set(false);
      }
    });
  }

  goToGroup(id: number): void {
    this.router.navigate(['/groups', id]);
  }

  trackById(_: number, g: Group) { return g.id; }
}
