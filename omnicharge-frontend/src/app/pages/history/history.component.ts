import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterLink } from '@angular/router';
import { RechargeService, RechargeRequestDto } from '../../services/recharge.service';
import { PaymentService, TransactionDto } from '../../services/payment.service';

@Component({
  selector: 'app-history',
  standalone: true,
  imports: [CommonModule, RouterLink],
  templateUrl: './history.component.html'
})
export class HistoryComponent implements OnInit {
  activeTab = 'recharges';
  recharges: RechargeRequestDto[] = [];
  transactions: TransactionDto[] = [];
  loadingRecharges = true;
  loadingTransactions = true;

  constructor(
    private rechargeService: RechargeService,
    private paymentService: PaymentService
  ) {}

  ngOnInit(): void {
    this.rechargeService.getMyHistory().subscribe({
      next: (list) => { this.recharges = list; this.loadingRecharges = false; },
      error: () => this.loadingRecharges = false
    });
    this.paymentService.getMyTransactions().subscribe({
      next: (list) => { this.transactions = list; this.loadingTransactions = false; },
      error: () => this.loadingTransactions = false
    });
  }

  cancelRecharge(id: number): void {
    if (!confirm('Are you sure you want to cancel this recharge?')) return;
    this.rechargeService.cancelRecharge(id).subscribe({
      next: () => {
        this.rechargeService.getMyHistory().subscribe({
          next: (list) => this.recharges = list
        });
      },
      error: (err) => alert(err.error?.error || err.error?.message || 'Failed to cancel')
    });
  }
}
