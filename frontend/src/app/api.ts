import { Injectable } from '@angular/core';

import { Info, Resultado } from './modelos';

@Injectable({ providedIn: 'root' })
export class Api {
  async info(): Promise<Info> {
    const r = await fetch('/api/info');
    return r.json();
  }

  async extrair(arquivo: File): Promise<Resultado> {
    const form = new FormData();
    form.append('arquivo', arquivo);
    const r = await fetch('/api/extracoes', { method: 'POST', body: form });
    if (!r.ok) {
      let mensagem = `O servidor respondeu com erro ${r.status}.`;
      try {
        mensagem = (await r.json()).detail ?? mensagem;
      } catch {
        // corpo não era JSON
      }
      if (r.status === 413) {
        mensagem = 'O arquivo passa do limite de 10 MB.';
      }
      throw new Error(mensagem);
    }
    return r.json();
  }

  /** Busca um dos PDFs de exemplo servidos pela própria aplicação. */
  async exemplo(nome: string): Promise<File> {
    const r = await fetch(`exemplos/${nome}`);
    return new File([await r.blob()], nome, { type: 'application/pdf' });
  }
}
