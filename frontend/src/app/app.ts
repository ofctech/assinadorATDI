import { CommonModule } from '@angular/common';
import { HttpClient, HttpHeaders, HttpResponse } from '@angular/common/http';
import { Component, computed, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { finalize } from 'rxjs';
import { environment } from '../environments/environment';

interface LoginResponse {
  access_token?: string;
  token_type?: string;
  expires_in?: number;
  error?: string;
}

interface DistinguishedName {
  common_name?: string;
  country_name?: string;
  email_address?: string;
  nuit?: string;
  organization_name?: string;
  organizational_unit_name?: string;
  state_or_province_name?: string;
}

interface VerificationSummary {
  isValid: boolean;
  invalidCause: string;
  subject: DistinguishedName;
  issuer: DistinguishedName;
}

type Operation = 'sign' | 'verify';

@Component({
  selector: 'app-root',
  imports: [CommonModule, FormsModule],
  templateUrl: './app.html',
  styleUrl: './app.css',
})
export class App {
  private readonly apiBaseUrl = environment.apiBaseUrl;
  private readonly maxPdfBytes = 3 * 1024 * 1024;

  username = '';
  password = '';
  sendEmail = true;
  attachSignedDocument = true;

  signFile = signal<File | null>(null);
  verifyFile = signal<File | null>(null);
  token = signal<string | null>(sessionStorage.getItem('assinador_token'));
  isLoggingIn = signal(false);
  activeOperation = signal<Operation | null>(null);
  activeMenu = signal<Operation>('sign');
  notice = signal('');
  error = signal('');
  verificationResult = signal<string>('');
  verificationSummaries = signal<VerificationSummary[]>([]);

  isAuthenticated = computed(() => Boolean(this.token()));
  userName = computed(() => this.decodeTokenValue('name') || this.decodeTokenValue('sub') || this.username);
  userEmail = computed(() => this.decodeTokenValue('email'));

  constructor(private readonly http: HttpClient) {}

  login(): void {
    this.clearMessages();
    this.clearResponses();

    if (!this.username || !this.password) {
      this.error.set('Informe o utilizador e a palavra-passe.');
      return;
    }

    const body = { username: this.username, password: this.password };

    this.isLoggingIn.set(true);
    this.http
      .post<LoginResponse>(`${this.apiBaseUrl}/api/ldapLogin/login`, body, {
        headers: new HttpHeaders({ 'Content-Type': 'application/json' }),
      })
      .pipe(finalize(() => this.isLoggingIn.set(false)))
      .subscribe({
        next: (response) => {
          if (!response.access_token) {
            this.error.set('A resposta de autenticação não trouxe token.');
            return;
          }

          this.token.set(response.access_token);
          sessionStorage.setItem('assinador_token', response.access_token);
          this.password = '';
          this.notice.set('Sessão iniciada.');
        },
        error: (err) => {
          this.error.set(this.extractErrorMessage(err, 'Não foi possível autenticar.'));
        },
      });
  }

  logout(): void {
    this.token.set(null);
    sessionStorage.removeItem('assinador_token');
    this.clearMessages();
    this.clearResponses();
  }

  selectMenu(operation: Operation): void {
    this.activeMenu.set(operation);
    this.clearMessages();
    this.clearResponses();
  }

  clearForm(operation: Operation, input?: HTMLInputElement): void {
    this.setOperationFile(operation, null);
    if (input) {
      input.value = '';
    }
    this.clearMessages();

    if (operation === 'verify') {
      this.verificationResult.set('');
      this.verificationSummaries.set([]);
    }
  }

  selectFile(event: Event, operation: Operation): void {
    this.clearMessages();
    const input = event.target as HTMLInputElement;
    const file = input.files?.[0] ?? null;

    if (!file) {
      this.setOperationFile(operation, null);
      return;
    }

    const validationError = this.validatePdf(file);
    if (validationError) {
      this.setOperationFile(operation, null);
      input.value = '';
      this.error.set(validationError);
      return;
    }

    this.setOperationFile(operation, file);
  }

  signDocument(fileInput: HTMLInputElement): void {
    const file = this.signFile();
    if (!this.ensureReady(file)) {
      return;
    }

    const formData = new FormData();
    formData.append('file', file);
    formData.append('send_email', String(this.sendEmail));
    formData.append('attach_signed_document', String(this.attachSignedDocument));

    this.activeOperation.set('sign');
    this.http
      .post(`${this.apiBaseUrl}/api/signer/sign`, formData, this.responseOptions())
      .pipe(finalize(() => this.clearOperationFile('sign', fileInput)))
      .subscribe({
        next: (response) => this.handleOperationResponse(response, 'documento-assinado.pdf'),
        error: (err) => this.error.set(this.extractErrorMessage(err, 'Não foi possível assinar o documento.')),
      });
  }

  verifyDocument(fileInput: HTMLInputElement): void {
    const file = this.verifyFile();
    if (!this.ensureReady(file)) {
      return;
    }

    const formData = new FormData();
    formData.append('file', file);

    this.activeOperation.set('verify');
    this.verificationResult.set('');
    this.verificationSummaries.set([]);
    this.http
      .post(`${this.apiBaseUrl}/api/signer/verify`, formData, this.responseOptions())
      .pipe(finalize(() => this.clearOperationFile('verify', fileInput)))
      .subscribe({
        next: (response) => this.handleVerifyResponse(response),
        error: (err) => this.error.set(this.extractErrorMessage(err, 'Não foi possível validar o documento.')),
      });
  }

  private clearOperationFile(operation: Operation, input: HTMLInputElement): void {
    this.activeOperation.set(null);
    this.setOperationFile(operation, null);
    input.value = '';
  }

  private responseOptions() {
    return {
      headers: new HttpHeaders({ Authorization: `Bearer ${this.token()}` }),
      observe: 'response' as const,
      responseType: 'blob' as const,
    };
  }

  private handleOperationResponse(response: HttpResponse<Blob>, downloadFallbackName?: string): void {
    const contentType = response.headers.get('Content-Type') ?? '';
    const body = response.body;

    if (!body) {
      this.notice.set('Operação concluída sem conteúdo de resposta.');
      return;
    }

    if (contentType.includes('application/pdf')) {
      this.downloadBlob(body, this.resolveDownloadName(response) || downloadFallbackName || 'documento.pdf');
      this.notice.set('Documento processado com sucesso.');
      return;
    }

    body.text().then((text) => {
      this.verificationResult.set(this.formatResponseText(text));
      this.notice.set('Operação concluída.');
    });
  }

  private handleVerifyResponse(response: HttpResponse<Blob>): void {
    const body = response.body;

    if (!body) {
      this.notice.set('Validação concluída sem conteúdo de resposta.');
      return;
    }

    body.text().then((text) => {
      try {
        const data = JSON.parse(text);
        const items = Array.isArray(data) ? data : [data];
        const summaries = items.map((item): VerificationSummary => {
          const validity = item?.signature?.validity;
          return {
            isValid: Boolean(validity?.is_valid),
            invalidCause: validity?.invalid_cause ?? '',
            subject: item?.certificate?.subject ?? {},
            issuer: item?.certificate?.issuer ?? {},
          };
        });

        this.verificationSummaries.set(summaries);
        this.verificationResult.set('');

        const validCount = summaries.filter((summary) => summary.isValid).length;
        const invalidCount = summaries.length - validCount;

        if (invalidCount === 0) {
          this.notice.set(
            summaries.length === 1
              ? 'Documento validado com sucesso. A assinatura é válida.'
              : `Documento validado com sucesso. ${validCount} assinaturas válidas.`,
          );
          return;
        }

        this.error.set(
          validCount > 0
            ? `Validação concluída com ${validCount} assinatura(s) válida(s) e ${invalidCount} inválida(s).`
            : 'Nenhuma assinatura válida foi encontrada no documento.',
        );
      } catch {
        this.verificationSummaries.set([]);
        this.verificationResult.set(this.formatResponseText(text));
        this.notice.set('Validação concluída.');
      }
    });
  }

  private ensureReady(file: File | null): file is File {
    this.clearMessages();

    if (!this.token()) {
      this.error.set('Inicie sessão antes de continuar.');
      return false;
    }

    if (!file) {
      this.error.set('Selecione um ficheiro PDF.');
      return false;
    }

    const validationError = this.validatePdf(file);
    if (validationError) {
      this.error.set(validationError);
      return false;
    }

    return true;
  }

  private validatePdf(file: File): string {
    const isPdf = file.type === 'application/pdf' || file.name.toLowerCase().endsWith('.pdf');
    if (!isPdf) {
      return 'O anexo deve ser um ficheiro PDF.';
    }

    if (file.size > this.maxPdfBytes) {
      return 'O anexo não pode exceder 3MB.';
    }

    return '';
  }

  private setOperationFile(operation: Operation, file: File | null): void {
    if (operation === 'sign') {
      this.signFile.set(file);
      return;
    }

    this.verifyFile.set(file);
  }

  private downloadBlob(blob: Blob, filename: string): void {
    const url = URL.createObjectURL(blob);
    const link = document.createElement('a');
    link.href = url;
    link.download = filename;
    link.click();
    URL.revokeObjectURL(url);
  }

  private resolveDownloadName(response: HttpResponse<Blob>): string {
    const disposition = response.headers.get('Content-Disposition') ?? '';
    const match = disposition.match(/filename="?([^";]+)"?/i);
    return match?.[1] ?? '';
  }

  private formatResponseText(text: string): string {
    if (!text) {
      return 'Sem conteúdo de resposta.';
    }

    try {
      return JSON.stringify(JSON.parse(text), null, 2);
    } catch {
      return text;
    }
  }

  private extractErrorMessage(err: unknown, fallback: string): string {
    const maybeError = err as { error?: Blob | { message?: string; error?: string }; message?: string };

    if (maybeError.error instanceof Blob) {
      maybeError.error.text().then((text) => this.error.set(this.formatResponseText(text)));
      return fallback;
    }

    if (maybeError.error && !(maybeError.error instanceof Blob)) {
      return maybeError.error.message || maybeError.error.error || fallback;
    }

    return maybeError.message || fallback;
  }

  private clearMessages(): void {
    this.notice.set('');
    this.error.set('');
  }

  private clearResponses(): void {
    this.verificationResult.set('');
    this.verificationSummaries.set([]);
  }

  private decodeTokenValue(key: string): string {
    const value = this.token();
    if (!value) {
      return '';
    }

    try {
      const payload = JSON.parse(atob(value.split('.')[1]));
      return payload[key] ?? '';
    } catch {
      return '';
    }
  }
}
