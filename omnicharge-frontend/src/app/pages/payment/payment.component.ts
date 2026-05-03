import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { PaymentService, PaymentGatewayRequest, TransactionDto } from '../../services/payment.service';
import { RechargeService, RechargeRequestDto } from '../../services/recharge.service';
import { UserService } from '../../services/user.service';

@Component({
  selector: 'app-payment',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterLink],
  templateUrl: './payment.component.html',
  styleUrl: './payment.component.css'
})
export class PaymentComponent implements OnInit {
  rechargeId!: number;
  recharge: RechargeRequestDto | null = null;
  transaction: TransactionDto | null = null;
  loading = true;
  submitting = false;
  errorMessage = '';
  successMessage = '';
  paymentDone = false;

  // Wallet
  walletBalance: number = 0;
  showAddMoneyModal = false;
  addMoneyAmount: number = 0;
  addMoneyUpi: string = '';
  isProcessing = false;
  showSuccessPopup = false;
  lastTopupAmount: number = 0;

  // Validation
  fieldErrors: any = {
    cardNumber: '', cardExpiry: '', cardCvv: '', cardHolderName: '',
    upiId: '', bankCode: '', accountNumber: '',
    addMoneyAmount: '', addMoneyUpi: ''
  };

  showTimerPopup = false;
  countdown = 300;
  timerInterval: any;

  get formattedTimer() {
    const m = Math.floor(this.countdown / 60);
    const s = this.countdown % 60;
    return `${m < 10 ? '0'+m : m}:${s < 10 ? '0'+s : s}`;
  }

  paymentMethod = '';
  // CARD
  cardNumber = '';
  cardExpiry = '';
  cardCvv = '';
  cardHolderName = '';
  // UPI
  upiId = '';
  // NETBANKING
  bankCode = '';
  accountNumber = '';
  validBanks = ['SBI', 'HDFC', 'ICICI', 'AXIS', 'KOTAK', 'BOB', 'PNB', 'CANARA', 'UNION', 'INDUSIND'];

  constructor(
    private route: ActivatedRoute,
    private router: Router,
    private paymentService: PaymentService,
    private rechargeService: RechargeService,
    private userService: UserService
  ) {}

  ngOnInit(): void {
    const rId = this.route.snapshot.paramMap.get('rechargeId');
    if (rId) {
      this.rechargeId = +rId;
      this.loadRechargeDetails();
    }
    this.loadWalletBalance();
  }

  loadWalletBalance(): void {
    this.userService.getWalletBalance().subscribe({
      next: (b) => this.walletBalance = b,
      error: (err) => console.error('Failed to load wallet balance', err)
    });
  }

  loadRechargeDetails(): void {
    this.rechargeService.getRechargeById(this.rechargeId).subscribe({
      next: (r) => { this.recharge = r; this.loading = false; },
      error: (err) => {
        this.loading = false;
        this.errorMessage = err.error?.error || 'Failed to load recharge details';
      }
    });
  }

  validate(): boolean {
    this.fieldErrors = {}; // Clear old errors
    let isValid = true;

    if (!this.paymentMethod) {
      this.errorMessage = 'Please select a payment method';
      return false;
    }

    switch (this.paymentMethod) {
      case 'CARD':
        if (!this.cardNumber || !/^\d{16}$/.test(this.cardNumber)) { this.fieldErrors.cardNumber = 'Invalid card number (16 digits)'; isValid = false; }
        if (!this.cardExpiry || !/^(0[1-9]|1[0-2])\/\d{2}$/.test(this.cardExpiry)) { this.fieldErrors.cardExpiry = 'Invalid expiry (MM/YY)'; isValid = false; }
        if (!this.cardCvv || !/^\d{3}$/.test(this.cardCvv)) { this.fieldErrors.cardCvv = 'Invalid CVV (3 digits)'; isValid = false; }
        if (!this.cardHolderName || !this.cardHolderName.trim()) { this.fieldErrors.cardHolderName = 'Name is required'; isValid = false; }
        break;
      case 'UPI':
        if (!this.upiId || !/^[a-zA-Z0-9._%+\-]+@[a-zA-Z]{3,}$/.test(this.upiId)) { this.fieldErrors.upiId = 'Invalid UPI ID format (e.g. name@upi)'; isValid = false; }
        break;
      case 'NETBANKING':
        if (!this.bankCode) { this.fieldErrors.bankCode = 'Bank selection is required'; isValid = false; }
        if (!this.accountNumber || !/^\d{9,18}$/.test(this.accountNumber)) { this.fieldErrors.accountNumber = 'Invalid account number (9-18 digits)'; isValid = false; }
        break;
      case 'WALLET':
        if (this.recharge && this.walletBalance < this.recharge.amount) {
          this.errorMessage = `Insufficient Wallet Balance (₹${this.walletBalance}). Please add money first.`;
          return false;
        }
        break;
    }
    return isValid;
  }

  submitPayment(): void {
    this.errorMessage = '';
    this.successMessage = '';

    if (!this.validate()) return;

    this.isProcessing = true;
    
    // Simulate processing delay for professional feel
    setTimeout(() => {
      const request: PaymentGatewayRequest = {
        rechargeId: this.rechargeId,
        paymentMethod: this.paymentMethod,
        cardNumber: this.cardNumber || undefined,
        cardExpiry: this.cardExpiry || undefined,
        cardCvv: this.cardCvv || undefined,
        cardHolderName: this.cardHolderName || undefined,
        upiId: this.upiId || undefined,
        bankCode: this.bankCode || undefined,
        accountNumber: this.accountNumber || undefined,
        walletType: 'OMNICHARGE', // For WALLET payments, we use our own
        walletMobile: '0000000000'
      };

      this.paymentService.makePayment(request).subscribe({
        next: (txn) => {
          this.isProcessing = false;
          this.transaction = txn;
          this.paymentDone = true;
          if (txn.status === 'SUCCESS') {
            this.successMessage = 'Payment successful! Reference: ' + txn.paymentReference;
            this.loadWalletBalance(); // Refresh balance after deduction
          } else {
            this.errorMessage = 'Payment failed: ' + (txn.failureReason || 'Unknown error');
          }
        },
        error: (err) => {
          this.isProcessing = false;
          this.errorMessage = err.error?.error || err.error?.message || 'Payment processing failed';
        }
      });
    }, 1500);
  }

  // ── Wallet Top-up Logic ───────────────────────────────────────────────────

  openAddMoney(): void {
    this.showAddMoneyModal = true;
    this.addMoneyAmount = 0;
    this.addMoneyUpi = '';
    // Do not override this.paymentMethod here so the background form is not affected
  }

  confirmTopUp(): void {
    this.fieldErrors = {};
    if (this.addMoneyAmount <= 0) { this.fieldErrors.addMoneyAmount = 'Please enter a valid amount'; return; }
    if (!this.addMoneyUpi || !/^[a-zA-Z0-9._%+\-]+@[a-zA-Z]{3,}$/.test(this.addMoneyUpi)) { this.fieldErrors.addMoneyUpi = 'Invalid UPI ID'; return; }

    this.isProcessing = true;
    this.showAddMoneyModal = false;

    const request: PaymentGatewayRequest = {
      rechargeId: -1,
      paymentMethod: 'UPI',
      upiId: this.addMoneyUpi,
      amount: this.addMoneyAmount
    };

    setTimeout(() => {
      this.paymentService.topUpWallet(request).subscribe({
        next: (txn) => {
          this.isProcessing = false;
          if (txn.status === 'SUCCESS') {
            this.lastTopupAmount = this.addMoneyAmount;
            this.showSuccessPopup = true;
            this.loadWalletBalance();
          } else {
            this.errorMessage = 'Top-up failed: ' + txn.failureReason;
          }
        },
        error: (err) => {
          this.isProcessing = false;
          this.errorMessage = 'Top-up failed. Please try again.';
        }
      });
    }, 1500);
  }

  closePopups(): void {
    this.showSuccessPopup = false;
    this.showAddMoneyModal = false;
    this.isProcessing = false;
  }
}
