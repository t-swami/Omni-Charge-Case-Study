import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterLink } from '@angular/router';
import { AuthService } from '../../services/auth.service';
import { UserService, UserDto } from '../../services/user.service';
import { RechargeService, RechargeRequestDto } from '../../services/recharge.service';
import { PaymentService, TransactionDto } from '../../services/payment.service';

@Component({
  selector: 'app-user-dashboard',
  standalone: true,
  imports: [CommonModule, RouterLink],
  templateUrl: './user-dashboard.component.html',
  styleUrls: ['./user-dashboard.component.css']
})
export class UserDashboardComponent implements OnInit {
  profile: UserDto | null = null;
  recharges: RechargeRequestDto[] = [];
  loading = true;

  constructor(
    public authService: AuthService,
    private userService: UserService,
    private rechargeService: RechargeService
  ) {}

  ngOnInit(): void {
    this.loadData();
  }

  loadData(): void {
    this.loading = true;
    this.userService.getProfile().subscribe({
      next: (p) => this.profile = p,
      error: () => {}
    });
    this.rechargeService.getMyHistory().subscribe({
      next: (list) => { this.recharges = list; this.loading = false; },
      error: () => this.loading = false
    });
  }

  getCountByStatus(status: string): number {
    return this.recharges.filter(r => r.status === status).length;
  }

  cancelRecharge(id: number): void {
    if (!confirm('Are you sure you want to cancel this recharge?')) return;
    this.rechargeService.cancelRecharge(id).subscribe({
      next: () => this.loadData(),
      error: (err) => alert(err.error?.error || err.error?.message || 'Failed to cancel recharge')
    });
  }
}
