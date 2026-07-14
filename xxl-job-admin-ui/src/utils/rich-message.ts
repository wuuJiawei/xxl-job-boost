const ALLOWED_TAGS = new Set(['BR', 'SPAN', 'B', 'STRONG', 'I', 'EM', 'CODE']);

function escapeHtml(value: string) {
  return value
    .replace(/&/g, '&amp;')
    .replace(/</g, '&lt;')
    .replace(/>/g, '&gt;')
    .replace(/"/g, '&quot;')
    .replace(/'/g, '&#39;');
}

function sanitizeColor(value: string) {
  const color = value.trim();
  if (/^#[0-9a-f]{3}([0-9a-f]{3})?$/i.test(color)) {
    return color;
  }
  if (/^rgb(a)?\(\s*\d{1,3}\s*,\s*\d{1,3}\s*,\s*\d{1,3}(\s*,\s*(0|1|0?\.\d+))?\s*\)$/i.test(color)) {
    return color;
  }
  if (/^[a-z]+$/i.test(color)) {
    return color;
  }
  return '';
}

function sanitizeStyle(style: string) {
  const declarations = style
    .split(';')
    .map(item => item.trim())
    .filter(Boolean);
  const safeDeclarations: string[] = [];

  declarations.forEach(declaration => {
    const [property, ...valueParts] = declaration.split(':');
    if (property?.trim().toLowerCase() !== 'color') {
      return;
    }
    const color = sanitizeColor(valueParts.join(':'));
    if (color) {
      safeDeclarations.push(`color:${color}`);
    }
  });

  return safeDeclarations.join(';');
}

function sanitizeNode(node: Node): string {
  if (node.nodeType === Node.TEXT_NODE) {
    return escapeHtml(node.textContent || '');
  }

  if (node.nodeType !== Node.ELEMENT_NODE) {
    return '';
  }

  const element = node as HTMLElement;
  const tagName = element.tagName.toUpperCase();
  const children = Array.from(element.childNodes).map(sanitizeNode).join('');

  if (!ALLOWED_TAGS.has(tagName)) {
    return children;
  }

  if (tagName === 'BR') {
    return '<br>';
  }

  const tag = tagName.toLowerCase();
  if (tagName === 'SPAN') {
    const safeStyle = sanitizeStyle(element.getAttribute('style') || '');
    return safeStyle ? `<span style="${safeStyle}">${children}</span>` : `<span>${children}</span>`;
  }

  return `<${tag}>${children}</${tag}>`;
}

export function sanitizeRichMessage(value: string) {
  if (!value) {
    return '';
  }
  if (typeof document === 'undefined') {
    return escapeHtml(value).replace(/&lt;br\s*\/?&gt;/gi, '<br>');
  }

  const template = document.createElement('template');
  template.innerHTML = value;
  return Array.from(template.content.childNodes).map(sanitizeNode).join('');
}
