import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

export interface InitiateRechargeRequest {
  mobileNumber: string;
  operatorId: number;
  planId: number;
}

export interface RechargeRequestDto {
  id: number;
  username: string;
  mobileNumber: string;
  operatorId: number;
  operatorName: string;
  planId: number;
  planName: string;
  amount: number;
  validity: string;
  dataInfo: string;
  status: string;
  createdAt: string;
  updatedAt: string;
  failureReason: string;
}

@Injectable({ providedIn: 'root' })
export class RechargeService {
  private apiUrl = '/api/recharge';

  constructor(private http: HttpClient) {}

  initiateRecharge(request: InitiateRechargeRequest): Observable<RechargeRequestDto> {
    return this.http.post<RechargeRequestDto>(`${this.apiUrl}/initiate`, request);
  }

  cancelRecharge(id: number): Observable<RechargeRequestDto> {
    return this.http.put<RechargeRequestDto>(`${this.apiUrl}/${id}/cancel`, {});
  }

  getMyHistory(): Observable<RechargeRequestDto[]> {
    return this.http.get<RechargeRequestDto[]>(`${this.apiUrl}/my-history`);
  }

  getRechargeById(id: number): Observable<RechargeRequestDto> {
    return this.http.get<RechargeRequestDto>(`${this.apiUrl}/${id}`);
  }

  getAllRecharges(): Observable<RechargeRequestDto[]> {
    return this.http.get<RechargeRequestDto[]>(`${this.apiUrl}/all`);
  }

  getRechargesByStatus(status: string): Observable<RechargeRequestDto[]> {
    return this.http.get<RechargeRequestDto[]>(`${this.apiUrl}/status/${status}`);
  }

  getRechargesByMobile(mobileNumber: string): Observable<RechargeRequestDto[]> {
    return this.http.get<RechargeRequestDto[]>(`${this.apiUrl}/mobile/${mobileNumber}`);
  }
}
