/** Formatação para exibir: a API devolve só dígitos, a tela mostra como no papel. */

export function documento(d: string | null | undefined): string {
  if (!d) {
    return '—';
  }
  const s = d.replace(/[.\-/\s]/g, '');
  if (/^\d{11}$/.test(s)) {
    return `${s.slice(0, 3)}.${s.slice(3, 6)}.${s.slice(6, 9)}-${s.slice(9)}`;
  }
  if (/^[0-9A-Za-z]{12}\d{2}$/.test(s)) {
    return `${s.slice(0, 2)}.${s.slice(2, 5)}.${s.slice(5, 8)}/${s.slice(8, 12)}-${s.slice(12)}`;
  }
  return d;
}

export function chaveNfe(c: string | null | undefined): string {
  return c ? (c.match(/.{1,4}/g) ?? []).join(' ') : '—';
}

export function linhaDigitavel(l: string | null | undefined): string {
  if (!l) {
    return '—';
  }
  if (l.length !== 47) {
    return l;
  }
  return `${l.slice(0, 5)}.${l.slice(5, 10)} ${l.slice(10, 15)}.${l.slice(15, 21)} ${l.slice(21, 26)}.${l.slice(26, 32)} ${l[32]} ${l.slice(33)}`;
}

const BRL = new Intl.NumberFormat('pt-BR', { style: 'currency', currency: 'BRL' });

export function moeda(v: number | null | undefined): string {
  return v == null ? '—' : BRL.format(v);
}

export function data(iso: string | null | undefined): string {
  if (!iso) {
    return '—';
  }
  const [a, m, d] = iso.split('-');
  return `${d}/${m}/${a}`;
}
