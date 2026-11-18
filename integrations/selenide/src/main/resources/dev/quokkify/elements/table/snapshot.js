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

// Close to Selenium's isShown, so snapshot text agrees with getText(): no box, visibility:hidden or opacity:0
// (on the element or an ancestor) is hidden; a display:contents element has no box of its own and is shown when
// any child is.
function visible(el) {
  const style = getComputedStyle(el);
  if (style.display === 'contents') {
    return Array.from(el.children).some(visible);
  }
  if (typeof el.checkVisibility === 'function') {
    return el.checkVisibility({checkOpacity: true, checkVisibilityCSS: true}) && el.getClientRects().length > 0;
  }
  return el.getClientRects().length > 0 && style.visibility !== 'hidden' && Number(style.opacity) > 0;
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
