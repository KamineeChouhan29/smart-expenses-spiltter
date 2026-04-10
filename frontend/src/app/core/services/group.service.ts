import { Injectable, signal } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, tap } from 'rxjs';
import { BalanceEntry, Group, GroupMember } from '../models/models';

@Injectable({ providedIn: 'root' })
export class GroupService {
  private readonly API = 'http://localhost:8080/api/groups';

  // Reactive signals for real-time dashboard updates
  groups = signal<Group[]>([]);

  constructor(private http: HttpClient) {}

  loadGroups(): Observable<Group[]> {
    return this.http.get<Group[]>(this.API)
      .pipe(tap(g => this.groups.set(g)));
  }

  createGroup(name: string): Observable<Group> {
    return this.http.post<Group>(this.API, { name })
      .pipe(tap(g => this.groups.update(list => [g, ...list])));
  }

  addMember(groupId: number, username: string): Observable<void> {
    return this.http.post<void>(`${this.API}/${groupId}/members`, { username });
  }

  getMembers(groupId: number): Observable<GroupMember[]> {
    return this.http.get<GroupMember[]>(`${this.API}/${groupId}/members`);
  }

  getBalances(groupId: number): Observable<BalanceEntry[]> {
    return this.http.get<BalanceEntry[]>(`${this.API}/${groupId}/balances`);
  }

  updateGroup(groupId: number, name: string): Observable<Group> {
    return this.http.put<Group>(`${this.API}/${groupId}`, { name })
      .pipe(tap(updated => this.groups.update(list => list.map(g => g.id === groupId ? updated : g))));
  }

  deleteGroup(groupId: number): Observable<void> {
    return this.http.delete<void>(`${this.API}/${groupId}`)
      .pipe(tap(() => this.groups.update(list => list.filter(g => g.id !== groupId))));
  }
}
