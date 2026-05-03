import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

export interface PaymentGatewayRequest {
  rechargeId: number;
  paymentMethod: string;
  // CARD fields
  cardNumber?: string;
  cardExpiry?: string;
  cardCvv?: string;
  cardHolderName?: string;
  // UPI fields
  upiId?: string;
  // NETBANKING fields
  bankCode?: string;
  accountNumber?: string;
  // WALLET fields
  walletType?: string;
  walletMobile?: string;
  amount?: number;
}

export interface TransactionDto {
  id: number;
  transactionId: string;
  rechargeId: number;
  username: string;
  mobileNumber: string;
  operatorName: string;
  planName: string;
  amount: number;
  validity: string;
  dataInfo: string;
  status: string;
  paymentMethod: string;
  paymentReference: string;
  failureReason: string;
  createdAt: string;
  updatedAt: string;
}

@Injectable({ providedIn: 'root' })
export class PaymentService {
  private apiUrl = '/api/transactions';

  constructor(private http: HttpClient) {}

  makePayment(request: PaymentGatewayRequest): Observable<TransactionDto> {
    return this.http.post<TransactionDto>(`${this.apiUrl}/pay`, request);
  }

  topUpWallet(request: PaymentGatewayRequest): Observable<TransactionDto> {
    return this.http.post<TransactionDto>(`${this.apiUrl}/wallet/topup`, request);
  }

  getMyTransactions(): Observable<TransactionDto[]> {
    return this.http.get<TransactionDto[]>(`${this.apiUrl}/my-transactions`);
  }

  getByTransactionId(transactionId: string): Observable<TransactionDto> {
    return this.http.get<TransactionDto>(`${this.apiUrl}/txn/${transactionId}`);
  }

  getByRechargeId(rechargeId: number): Observable<TransactionDto> {
    return this.http.get<TransactionDto>(`${this.apiUrl}/recharge/${rechargeId}`);
  }

  getAllTransactions(): Observable<TransactionDto[]> {
    return this.http.get<TransactionDto[]>(`${this.apiUrl}/all`);
  }

  getTransactionsByStatus(status: string): Observable<TransactionDto[]> {
    return this.http.get<TransactionDto[]>(`${this.apiUrl}/status/${status}`);
  }

  getTransactionsByMobile(mobileNumber: string): Observable<TransactionDto[]> {
    return this.http.get<TransactionDto[]>(`${this.apiUrl}/mobile/${mobileNumber}`);
  }
}
