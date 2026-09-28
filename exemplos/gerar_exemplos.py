"""Gera os documentos de exemplo: uma nota fiscal, um boleto e um boleto adulterado.

Tudo é fictício. Os CNPJs têm dígito verificador válido, mas foram conferidos na
BrasilAPI e não pertencem a nenhuma empresa (resposta 404). Cada página leva a
marca "DOCUMENTO FICTÍCIO - SEM VALOR FISCAL".

Uso: python exemplos/gerar_exemplos.py   (requer reportlab)
"""
from datetime import date
from pathlib import Path

from reportlab.lib import colors
from reportlab.lib.pagesizes import A4
from reportlab.lib.units import mm
from reportlab.pdfgen import canvas

PASTA = Path(__file__).parent

EMITENTE = ("Papelaria Exemplo Ltda", "90770487000184", "Av. Exemplo, 100 - Centro - Cuiabá/MT")
DESTINATARIO = ("Empresa Exemplo Ltda", "90216739000128", "Rua Fictícia, 200 - Cuiabá/MT")
BENEFICIARIO = ("Escola de Idiomas Exemplo Ltda", "90126225000181")


# ---------- dígitos verificadores ----------

def dv_mod11_nfe(numero: str) -> str:
    pesos, soma = 2, 0
    for d in reversed(numero):
        soma += int(d) * pesos
        pesos = 2 if pesos == 9 else pesos + 1
    resto = soma % 11
    return "0" if resto < 2 else str(11 - resto)


def dv_mod10(numero: str) -> str:
    soma, peso = 0, 2
    for d in reversed(numero):
        p = int(d) * peso
        soma += p // 10 + p % 10
        peso = 1 if peso == 2 else 2
    return str((10 - soma % 10) % 10)


def dv_mod11_boleto(numero43: str) -> str:
    soma, peso = 0, 2
    for d in reversed(numero43):
        soma += int(d) * peso
        peso = 2 if peso == 9 else peso + 1
    dv = 11 - soma % 11
    return "1" if dv in (0, 10, 11) else str(dv)


def fator_vencimento(venc: date) -> str:
    # Desde 22/02/2025 o fator recomeçou em 1000 (a contagem de 1997 chegou a 9999).
    return str(1000 + (venc - date(2025, 2, 22)).days).zfill(4)


def linha_digitavel(banco: str, venc: date, valor_centavos: int, campo_livre: str) -> str:
    fator, valor = fator_vencimento(venc), str(valor_centavos).zfill(10)
    sem_dv = banco + "9" + fator + valor + campo_livre
    codigo = banco + "9" + dv_mod11_boleto(sem_dv) + fator + valor + campo_livre
    c1 = banco + "9" + campo_livre[:5]
    c2, c3 = campo_livre[5:15], campo_livre[15:25]
    return (c1 + dv_mod10(c1)) + (c2 + dv_mod10(c2)) + (c3 + dv_mod10(c3)) + codigo[4] + fator + valor


def fmt_linha(l: str) -> str:
    return f"{l[0:5]}.{l[5:10]} {l[10:15]}.{l[15:21]} {l[21:26]}.{l[26:32]} {l[32]} {l[33:]}"


def fmt_cnpj(c: str) -> str:
    return f"{c[:2]}.{c[2:5]}.{c[5:8]}/{c[8:12]}-{c[12:]}"


def brl(centavos: int) -> str:
    return f"R$ {centavos / 100:,.2f}".replace(",", "X").replace(".", ",").replace("X", ".")


# ---------- desenho ----------

def marca_dagua(c: canvas.Canvas):
    c.saveState()
    c.setFillColor(colors.Color(0.85, 0.1, 0.1, alpha=0.18))
    c.setFont("Helvetica-Bold", 38)
    c.translate(105 * mm, 150 * mm)
    c.rotate(35)
    c.drawCentredString(0, 0, "DOCUMENTO FICTÍCIO - SEM VALOR FISCAL")
    c.restoreState()


def caixa(c, x, y, w, h, rotulo, valor, tam=10):
    c.rect(x * mm, y * mm, w * mm, h * mm)
    c.setFont("Helvetica", 6.5)
    c.drawString((x + 1.5) * mm, (y + h - 3.2) * mm, rotulo)
    c.setFont("Helvetica-Bold", tam)
    c.drawString((x + 1.5) * mm, (y + 2) * mm, valor)


def nota_fiscal():
    emissao = date(2026, 9, 15)
    itens = [("Caderno universitário 10 matérias", 20, 2490),
             ("Caneta esferográfica azul, caixa com 50", 4, 4550),
             ("Papel A4, resma com 500 folhas", 10, 3275)]
    produtos = sum(q * v for _, q, v in itens)
    frete, desconto = 3500, 4250
    total = produtos + frete - desconto
    numero, serie = "000001234", "001"
    chave_sem_dv = "51" + emissao.strftime("%y%m") + EMITENTE[1] + "55" + serie + numero + "1" + "12345678"
    chave = chave_sem_dv + dv_mod11_nfe(chave_sem_dv)

    c = canvas.Canvas(str(PASTA / "nota-fiscal-exemplo.pdf"), pagesize=A4)
    c.setTitle("DANFE - Nota fiscal fictícia")
    marca_dagua(c)
    c.setFont("Helvetica-Bold", 14)
    c.drawString(15 * mm, 280 * mm, "DANFE")
    c.setFont("Helvetica", 8)
    c.drawString(15 * mm, 276 * mm, "Documento Auxiliar da Nota Fiscal Eletrônica  -  1 - Saída")
    c.drawString(150 * mm, 280 * mm, f"Nº {int(numero):,}".replace(",", "."))
    c.drawString(150 * mm, 276 * mm, f"Série {serie}")

    caixa(c, 15, 255, 110, 16, "EMITENTE", EMITENTE[0], 11)
    c.setFont("Helvetica", 8)
    c.drawString(17 * mm, 252 * mm, EMITENTE[2])
    caixa(c, 127, 255, 68, 16, "CNPJ DO EMITENTE", fmt_cnpj(EMITENTE[1]))
    grupos = " ".join(chave[i:i + 4] for i in range(0, 44, 4))
    caixa(c, 15, 235, 180, 12, "CHAVE DE ACESSO", grupos, 10)

    c.setFont("Helvetica-Bold", 9)
    c.drawString(15 * mm, 228 * mm, "DESTINATÁRIO / REMETENTE")
    caixa(c, 15, 214, 110, 12, "NOME / RAZÃO SOCIAL", DESTINATARIO[0])
    caixa(c, 127, 214, 40, 12, "CNPJ / CPF", fmt_cnpj(DESTINATARIO[1]), 9)
    caixa(c, 169, 214, 26, 12, "DATA DA EMISSÃO", emissao.strftime("%d/%m/%Y"), 9)

    c.setFont("Helvetica-Bold", 9)
    c.drawString(15 * mm, 206 * mm, "DADOS DOS PRODUTOS / SERVIÇOS")
    y = 200
    c.setFont("Helvetica-Bold", 7)
    for x, t in [(15, "DESCRIÇÃO"), (120, "QTD."), (140, "V. UNITÁRIO"), (170, "V. TOTAL")]:
        c.drawString(x * mm, y * mm, t)
    c.setFont("Helvetica", 8.5)
    for desc, q, v in itens:
        y -= 6
        c.drawString(15 * mm, y * mm, desc)
        c.drawString(120 * mm, y * mm, str(q))
        c.drawString(140 * mm, y * mm, brl(v))
        c.drawString(170 * mm, y * mm, brl(q * v))

    c.setFont("Helvetica-Bold", 9)
    c.drawString(15 * mm, 160 * mm, "CÁLCULO DO IMPOSTO")
    caixa(c, 15, 145, 44, 12, "VALOR TOTAL DOS PRODUTOS", brl(produtos))
    caixa(c, 61, 145, 44, 12, "VALOR DO FRETE", brl(frete))
    caixa(c, 107, 145, 44, 12, "DESCONTO", brl(desconto))
    caixa(c, 153, 145, 42, 12, "VALOR TOTAL DA NOTA", brl(total))
    c.showPage()
    c.save()
    return chave, total


def boleto(arquivo, banco_impresso, banco_linha, valor_impresso, valor_linha, campo_livre, titulo):
    venc = date(2026, 10, 10)
    linha = linha_digitavel(banco_linha, venc, valor_linha, campo_livre)
    c = canvas.Canvas(str(PASTA / arquivo), pagesize=A4)
    c.setTitle(titulo)
    marca_dagua(c)
    c.setFont("Helvetica-Bold", 13)
    c.drawString(15 * mm, 270 * mm, {"001": "Banco do Brasil", "237": "Bradesco"}[banco_impresso])
    c.setFont("Helvetica-Bold", 14)
    c.drawString(70 * mm, 270 * mm, f"| {banco_impresso}-9 |")
    c.setFont("Helvetica-Bold", 10)
    c.drawRightString(195 * mm, 270 * mm, fmt_linha(linha))
    c.line(15 * mm, 267 * mm, 195 * mm, 267 * mm)
    caixa(c, 15, 250, 130, 13, "LOCAL DE PAGAMENTO", "Pagável em qualquer banco até o vencimento", 9)
    caixa(c, 147, 250, 48, 13, "VENCIMENTO", venc.strftime("%d/%m/%Y"))
    caixa(c, 15, 235, 130, 13, "BENEFICIÁRIO", f"{BENEFICIARIO[0]} - CNPJ {fmt_cnpj(BENEFICIARIO[1])}", 8.5)
    caixa(c, 147, 235, 48, 13, "NOSSO NÚMERO", "00012345-6")
    caixa(c, 15, 220, 60, 13, "DATA DO DOCUMENTO", "25/09/2026")
    caixa(c, 77, 220, 68, 13, "Nº DO DOCUMENTO", "MENS-2026-10")
    caixa(c, 147, 220, 48, 13, "(=) VALOR DO DOCUMENTO", brl(valor_impresso))
    caixa(c, 15, 195, 180, 22, "INSTRUÇÕES",
          "Mensalidade de outubro de 2026. Não receber após 30 dias do vencimento.", 8.5)
    caixa(c, 15, 178, 180, 14, "PAGADOR", f"{DESTINATARIO[0]} - CNPJ {fmt_cnpj(DESTINATARIO[1])}", 9)
    c.showPage()
    c.save()
    return linha


if __name__ == "__main__":
    chave, total = nota_fiscal()
    print("nota fiscal: chave", chave, "total", brl(total))
    print("boleto:", boleto("boleto-exemplo.pdf", "001", "001", 18900, 18900,
                            "0000001234567890123456717", "Boleto fictício"))
    # Golpe do boleto adulterado: o cabeçalho e o valor impresso são os originais,
    # mas a linha digitável foi trocada por outra, de outro banco e com outro valor.
    print("adulterado:", boleto("boleto-adulterado.pdf", "001", "237", 18900, 189000,
                                "3381260070000000123456789", "Boleto fictício adulterado"))
