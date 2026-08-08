import fs from "node:fs/promises";
import path from "node:path";
import { createRequire } from "node:module";

const [inputPath, outputDirectory, nodeModules] = process.argv.slice(2);
if (!inputPath || !outputDirectory || !nodeModules) {
  throw new Error("Usage: node build_body_hud_assets.mjs <master.png> <out-dir> <node_modules>");
}

const require = createRequire(import.meta.url);
const sharp = require(path.join(nodeModules, "sharp"));
const outputSize = 256;

const bodyParts = [
  { key: "head", color: "#aeb6bd", polygon: [[325, 70], [690, 70], [690, 345], [325, 345]] },
  { key: "chest", color: "#ffbe35", polygon: [[350, 470], [690, 470], [690, 650], [350, 650]] },
  { key: "abdomen", color: "#ff7035", polygon: [[350, 625], [700, 625], [700, 755], [350, 755]] },
  { key: "right_arm", color: "#e84a3c", polygon: [[280, 305], [405, 370], [655, 370], [625, 550], [360, 520], [250, 430]] },
  { key: "left_arm", color: "#65b8ff", polygon: [[745, 385], [930, 385], [930, 535], [735, 535]] },
  { key: "right_leg", color: "#ca4cff", polygon: [[285, 710], [535, 710], [525, 1150], [275, 1150]] },
  { key: "left_leg", color: "#46d68a", polygon: [[505, 710], [770, 710], [785, 1150], [525, 1150]] }
];

// The rifle pose puts the arms in front of the chest. A rectangular chest
// crop therefore cannot tell an arm pixel from the torso underneath it. Give
// every visible body pixel one anatomical owner, with foreground limbs taking
// precedence over the torso. This is deliberately separate from enum/render
// order so no health layer can ever share pixels with another one.
const ownershipPriority = [
  "head",
  "right_arm",
  "left_arm",
  "chest",
  "abdomen",
  "right_leg",
  "left_leg"
];
const partByKey = new Map(bodyParts.map(part => [part.key, part]));

function pointInPolygon(x, y, polygon) {
  let inside = false;
  for (let i = 0, j = polygon.length - 1; i < polygon.length; j = i++) {
    const [xi, yi] = polygon[i];
    const [xj, yj] = polygon[j];
    const intersects = (yi > y) !== (yj > y)
      && x < ((xj - xi) * (y - yi)) / (yj - yi) + xi;
    if (intersects) inside = !inside;
  }
  return inside;
}

function parseHexColor(value) {
  const match = /^#([0-9a-f]{2})([0-9a-f]{2})([0-9a-f]{2})$/i.exec(value);
  if (!match) throw new Error(`Invalid preview color: ${value}`);
  return match.slice(1).map(channel => Number.parseInt(channel, 16));
}

async function transformedPipeline(raw, width, height, bounds, kernel = sharp.kernel.lanczos3) {
  const croppedWidth = bounds.right - bounds.left + 1;
  const croppedHeight = bounds.bottom - bounds.top + 1;
  const padding = 22;
  const squareSize = Math.max(croppedWidth, croppedHeight) + padding * 2;
  const horizontalExtra = squareSize - croppedWidth - padding * 2;
  const verticalExtra = squareSize - croppedHeight - padding * 2;
  const left = padding + Math.floor(horizontalExtra / 2);
  const right = padding + Math.ceil(horizontalExtra / 2);
  const top = padding + Math.floor(verticalExtra / 2);
  const bottom = padding + Math.ceil(verticalExtra / 2);

  // Materialize the square canvas before resizing. Sharp otherwise reorders
  // extend after resize and produces a non-square texture.
  const squared = await sharp(raw, { raw: { width, height, channels: 4 } })
    .extract({
      left: bounds.left,
      top: bounds.top,
      width: croppedWidth,
      height: croppedHeight
    })
    .extend({ left, right, top, bottom, background: { r: 0, g: 0, b: 0, alpha: 0 } })
    .png()
    .toBuffer();

  return sharp(squared)
    .resize(outputSize, outputSize, { fit: "fill", kernel });
}

await fs.mkdir(outputDirectory, { recursive: true });
const { data, info } = await sharp(inputPath).ensureAlpha().raw().toBuffer({ resolveWithObject: true });

const bounds = { left: info.width, top: info.height, right: 0, bottom: 0 };
for (let y = 0; y < info.height; y++) {
  for (let x = 0; x < info.width; x++) {
    const alpha = data[(y * info.width + x) * 4 + 3];
    if (alpha <= 8) continue;
    bounds.left = Math.min(bounds.left, x);
    bounds.top = Math.min(bounds.top, y);
    bounds.right = Math.max(bounds.right, x);
    bounds.bottom = Math.max(bounds.bottom, y);
  }
}

await (await transformedPipeline(data, info.width, info.height, bounds))
  .png({ compressionLevel: 9, palette: false })
  .toFile(path.join(outputDirectory, "body_hud.png"));

const previewLayers = [];
const generatedMaskPaths = [];
const pixelOwners = new Int16Array(info.width * info.height);
pixelOwners.fill(-1);
const ownedPixelCounts = new Map(bodyParts.map(part => [part.key, 0]));

for (let y = 0; y < info.height; y++) {
  for (let x = 0; x < info.width; x++) {
    const pixelIndex = y * info.width + x;
    const offset = pixelIndex * 4;
    const alpha = data[offset + 3];
    const luminance = data[offset] * 0.2126 + data[offset + 1] * 0.7152 + data[offset + 2] * 0.0722;
    if (alpha <= 8 || luminance < 58) continue;

    for (const key of ownershipPriority) {
      const part = partByKey.get(key);
      if (!pointInPolygon(x + 0.5, y + 0.5, part.polygon)) continue;
      const ownerIndex = bodyParts.indexOf(part);
      pixelOwners[pixelIndex] = ownerIndex;
      ownedPixelCounts.set(key, ownedPixelCounts.get(key) + 1);
      break;
    }
  }
}

for (const part of bodyParts) {
  const mask = Buffer.alloc(data.length);
  const ownerIndex = bodyParts.indexOf(part);
  for (let y = 0; y < info.height; y++) {
    for (let x = 0; x < info.width; x++) {
      const offset = (y * info.width + x) * 4;
      if (pixelOwners[y * info.width + x] !== ownerIndex) continue;
      const alpha = data[offset + 3];
      // Status masks are deliberately white. Minecraft multiplies this layer
      // by the requested health color, producing the hard, saturated region
      // changes used by classic RTS unit wireframes instead of a weak tint on
      // top of the grayscale art.
      mask[offset] = 255;
      mask[offset + 1] = 255;
      mask[offset + 2] = 255;
      mask[offset + 3] = alpha;
    }
  }

  const maskPath = path.join(outputDirectory, `body_hud_${part.key}.png`);
  await (await transformedPipeline(mask, info.width, info.height, bounds, sharp.kernel.nearest))
    .png({ compressionLevel: 9, palette: false })
    .toFile(maskPath);
  generatedMaskPaths.push(maskPath);

  const { data: previewMask, info: previewInfo } = await sharp(maskPath)
    .ensureAlpha()
    .raw()
    .toBuffer({ resolveWithObject: true });
  const [previewRed, previewGreen, previewBlue] = parseHexColor(part.color);
  for (let index = 0; index < previewInfo.width * previewInfo.height; index++) {
    previewMask[index * 4] = previewRed;
    previewMask[index * 4 + 1] = previewGreen;
    previewMask[index * 4 + 2] = previewBlue;
  }
  const tinted = await sharp(previewMask, { raw: previewInfo }).png().toBuffer();
  previewLayers.push({ input: tinted, blend: "over" });
}

for (const part of bodyParts) {
  if (ownedPixelCounts.get(part.key) === 0) {
    throw new Error(`Body part has no owned pixels: ${part.key}`);
  }
}

// Audit the actual final-size PNGs, not only the source ownership map. This
// catches accidental overlap introduced by any future resize/filter change.
const finalCoverage = new Uint8Array(outputSize * outputSize);
for (const maskPath of generatedMaskPaths) {
  const { data: finalMask, info: finalInfo } = await sharp(maskPath)
    .ensureAlpha()
    .raw()
    .toBuffer({ resolveWithObject: true });
  if (finalInfo.width !== outputSize || finalInfo.height !== outputSize) {
    throw new Error(`Unexpected mask size: ${maskPath}`);
  }
  for (let index = 0; index < finalCoverage.length; index++) {
    if (finalMask[index * 4 + 3] === 0) continue;
    finalCoverage[index]++;
    if (finalCoverage[index] > 1) {
      throw new Error(`Final masks overlap at ${index % outputSize},${Math.floor(index / outputSize)}`);
    }
  }
}

await sharp({
  create: {
    width: outputSize,
    height: outputSize,
    channels: 4,
    background: { r: 10, g: 13, b: 15, alpha: 1 }
  }
})
  .composite([
    { input: path.join(outputDirectory, "body_hud.png"), blend: "over" },
    ...previewLayers
  ])
  .png({ compressionLevel: 9 })
  .toFile(path.join(outputDirectory, "body_hud_region_preview.png"));

console.log(`Wrote HUD base, ${bodyParts.length} mutually exclusive masks, and region preview to ${outputDirectory}`);
console.log("Final-size overlap audit: 0 pixels");
console.log(Object.fromEntries(ownedPixelCounts));
