const [root, rowsSpec, cellsSpec, headersSpec, itemsSpec] = arguments;

function find(context, spec) {
  if (spec.kind === 'xpath') {
    const result = document.evaluate(spec.expr, context, null, XPathResult.ORDERED_NODE_SNAPSHOT_TYPE, null);
    const nodes = [];
    for (let i = 0; i < result.snapshotLength; i++) {
      const node = result.snapshotItem(i);
      if (node.nodeType === Node.ELEMENT_NODE) {
        nodes.push(node);
      }
    }
    return nodes;
  }
  return Array.from(context.querySelectorAll(spec.expr));
}

function visible(el) {
  if (typeof el.checkVisibility === 'function' && !el.checkVisibility()) {
    return false;
  }
  return el.getClientRects().length > 0;
}

function text(el) {
  return visible(el) ? el.innerText : '';
}

return {
  headers: find(root, headersSpec).map(text),
  rows: find(root, rowsSpec).map(row => {
    const cells = find(row, cellsSpec);
    return {
      texts: cells.map(text),
      values: itemsSpec ? cells.map(cell => find(cell, itemsSpec).filter(visible).map(text)) : null
    };
  })
};
