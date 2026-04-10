import { Injectable, signal } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, tap } from 'rxjs';
import { Expense, ExpenseRequest } from '../models/models';

@Injectable({ providedIn: 'root' })
export class ExpenseService {
  private apiUrl = 'https://smart-expenses-spiltter.onrender.com/api/expenses';

  // Reactive signal — updates dashboard balance in real time
  expenses = signal<Expense[]>([]);

  constructor(private http: HttpClient) {}

  addExpense(req: ExpenseRequest): Observable<Expense> {
    return this.http.post<Expense>(this.API, req)
      .pipe(tap(e => this.expenses.update(list => [e, ...list])));
  }

  getGroupExpenses(groupId: number): Observable<Expense[]> {
    return this.http.get<Expense[]>(`${this.API}/group/${groupId}`)
      .pipe(tap(list => this.expenses.set(list)));
  }

  updateExpense(expenseId: number, req: ExpenseRequest): Observable<Expense> {
    return this.http.put<Expense>(`${this.API}/${expenseId}`, req)
      .pipe(tap(updated => this.expenses.update(list => list.map(e => e.id === expenseId ? updated : e))));
  }

  getInsights(): Observable<string[]> {
    return this.http.get<string[]>(`${this.API}/insights`);
  }

  deleteExpense(expenseId: number): Observable<void> {
    return this.http.delete<void>(`${this.API}/${expenseId}`)
      .pipe(tap(() => this.expenses.update(list => list.filter(e => e.id !== expenseId))));
  }
}
