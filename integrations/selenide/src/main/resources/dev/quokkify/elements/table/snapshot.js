// arguments: root element, then [kind, expr] for rows, cells and headers, then [kind, expr] or [] for items.
const [root, rowsSpec, cellsSpec, headersSpec, itemsArg] = arguments;
const itemsSpec = itemsArg.length === 2 ? itemsArg : null;

function find(context, [kind, expr]) {
  if (kind === "xpath") {
    const result = document.evaluate(
      expr,
      context,
      null,
      XPathResult.ORDERED_NODE_SNAPSHOT_TYPE,
      null,
    );
    const nodes = [];
    for (let i = 0; i < result.snapshotLength; i++) {
      const node = result.snapshotItem(i);
      if (node.nodeType === Node.ELEMENT_NODE) {
        nodes.push(node);
      }
    }
    return nodes;
  }
  return Array.from(context.querySelectorAll(expr));
}

// Close to Selenium's isShown, so snapshot text agrees with getText(): no box, visibility:hidden or opacity:0
// (on the element or an ancestor) is hidden; a display:contents element has no box of its own and is shown when
// any child is.
function visible(el) {
  const style = getComputedStyle(el);
  if (style.display === "contents") {
    return Array.from(el.children).some(visible);
  }
  if (typeof el.checkVisibility === "function") {
    return (
      el.checkVisibility({ checkOpacity: true, checkVisibilityCSS: true }) &&
      el.getClientRects().length > 0
    );
  }
  return (
    el.getClientRects().length > 0 &&
    style.visibility !== "hidden" &&
    Number(style.opacity) > 0
  );
}

// Visible text like Selenium's getText(): innerText alone keeps the text of opacity:0 descendants, so an element
// with child elements is read child by child with the same visibility rule. Whitespace is normalized by the caller.
function text(el) {
  if (!visible(el)) {
    return "";
  }
  if (!el.firstElementChild) {
    return el.innerText;
  }
  let result = "";
  for (const node of el.childNodes) {
    if (node.nodeType === Node.TEXT_NODE) {
      result += node.textContent;
    } else if (node.nodeType === Node.ELEMENT_NODE) {
      const inline = getComputedStyle(node).display.startsWith("inline");
      const part = node.tagName === "BR" ? " " : text(node);
      result += inline ? part : " " + part + " ";
    }
  }
  return result;
}

return {
  headers: find(root, headersSpec).map(text),
  rows: find(root, rowsSpec).map((row) => {
    const cells = find(row, cellsSpec);
    return {
      texts: cells.map(text),
      values: itemsSpec
        ? cells.map((cell) => find(cell, itemsSpec).filter(visible).map(text))
        : null,
    };
  }),
};
