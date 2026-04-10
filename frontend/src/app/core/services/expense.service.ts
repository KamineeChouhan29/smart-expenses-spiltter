import { Injectable, signal } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, tap } from 'rxjs';
import { Expense, ExpenseRequest } from '../models/models';

@Injectable({ providedIn: 'root' })
export class ExpenseService {
  private readonly API = 'http://localhost:8080/api/expenses';

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

  getInsights(): Observable<string[]> {
    return this.http.get<string[]>(`${this.API}/insights`);
  }
}
