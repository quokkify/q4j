// Prepended by SnapshotScript: `const isShown = <Selenium's isDisplayed atom>;` so visibility is exactly Selenium's.
// arguments: root element, then [kind, expr] for rows, cells and headers, then [kind, expr] or [] for items.
const [root, rowsSpec, cellsSpec, headersSpec, itemsArg] = arguments;
const itemsSpec = itemsArg.length === 2 ? itemsArg : null;

function find(context, [kind, expr]) {
  if (kind === "xpath") {
    let result;
    try {
      result = document.evaluate(
        expr,
        context,
        null,
        XPathResult.ORDERED_NODE_SNAPSHOT_TYPE,
        null,
      );
    } catch (e) {
      // Selenide fails fast, without retry, on an error whose message has its invalid-selector marker. The marker is
      // spelled with an escape: Selenium quotes this script in the message of every error of the call (a stale root
      // too), so a literal marker here would make Selenide fail fast on those as well.
      throw new Error("invalid\u0020selector: " + expr + ": " + e.message);
    }
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

function transform(text, style) {
  switch (style.textTransform) {
    case "uppercase":
      return text.toUpperCase();
    case "lowercase":
      return text.toLowerCase();
    case "capitalize":
      return text.replace(
        /(^|\s)(\p{L})/gu,
        (_, space, letter) => space + letter.toUpperCase(),
      );
    default:
      return text;
  }
}

// Visible text like Selenium's getText(): an element contributes its own text only when Selenium considers it shown,
// and its children are judged one by one, so a visible child of a visibility:hidden parent still counts. Block-level
// children are separated by spaces; whitespace is normalized by the caller.
function text(el) {
  const shown = isShown(el);
  const style = getComputedStyle(el);
  let result = "";
  for (const node of el.childNodes) {
    if (node.nodeType === Node.TEXT_NODE) {
      if (shown) {
        result += transform(node.textContent, style);
      }
    } else if (node.nodeType === Node.ELEMENT_NODE) {
      if (node.tagName === "BR") {
        result += shown ? " " : "";
        continue;
      }
      const part = text(node);
      result += getComputedStyle(node).display.startsWith("inline")
        ? part
        : " " + part + " ";
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
      items: itemsSpec
        ? cells.map((cell) =>
            find(cell, itemsSpec)
              .filter((item) => isShown(item))
              .map(text),
          )
        : null,
    };
  }),
};
