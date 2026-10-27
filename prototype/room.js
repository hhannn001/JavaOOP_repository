/* Original pixel room, drawn at 480 × 300 and scaled without smoothing. */
(() => {
  const canvas = document.getElementById('roomCanvas');
  const ctx    = canvas.getContext('2d');
  const colors = {
    wall: '#354f48', wallLight: '#3e5b50', shadow: '#1d302c',
    wood: '#795f41', woodLight: '#a18455', woodDark: '#443f2e',
    gold: '#d4b773', cream: '#ddd5ac', green: '#77936a'
  };

  function rect(x, y, w, h, color) {
    ctx.fillStyle = color;
    ctx.fillRect(Math.round(x), Math.round(y), Math.round(w), Math.round(h));
  }

  function polygon(points, color) {
    ctx.fillStyle = color;
    ctx.beginPath();
    points.forEach(([x, y], index) => index ? ctx.lineTo(x, y) : ctx.moveTo(x, y));
    ctx.closePath();
    ctx.fill();
  }

  function grain(x, y, w, h, count, palette, seed = 1) {
    let random = seed;
    for (let i = 0; i < count; i++) {
      random = (random * 1664525 + 1013904223) >>> 0;
      const px = x + random % w;
      random   = (random * 1664525 + 1013904223) >>> 0;
      const py = y + random % h;
      rect(px, py, 1 + random % 3, 1, palette[i % palette.length]);
    }
  }

  function book(x, y, width, height, color, lean = false) {
    if (lean) {
      polygon([[x, y], [x + width, y - 2], [x + width + 6, y + height], [x + 6, y + height + 2]], color);
    } else {
      rect(x, y, width, height, color);
      rect(x + 1, y + 3, width - 2, 1, '#b9ac7a');
      rect(x + 1, y + height - 4, width - 2, 1, '#b9ac7a');
      rect(x, y, 1, height, '#1e302b55');
    }
  }

  function plant(x, y, scale = 1) {
    ctx.save();
    ctx.translate(x, y);
    ctx.scale(scale, scale);
    rect(-2, -28, 3, 35, '#6b8760');
    polygon([[-1, -16], [-16, -21], [-21, -29], [-13, -31], [-4, -26]], '#779367');
    polygon([[0, -10], [14, -20], [19, -28], [9, -27], [1, -20]], '#91a471');
    polygon([[-2, -28], [-10, -36], [-9, -44], [-3, -41], [2, -33]], '#9bac79');
    polygon([[1, -24], [7, -37], [14, -40], [13, -31], [6, -23]], '#668360');
    polygon([[-1, -4], [-16, -10], [-19, -19], [-8, -16], [0, -9]], '#4f7354');
    polygon([[-12, 0], [12, 0], [8, 21], [-8, 21]], '#977653');
    rect(-13, -2, 26, 5, '#b29468');
    rect(-8, 5, 3, 13, '#aa885e');
    rect(-6, 20, 13, 2, '#3c4834');
    ctx.restore();
  }

  function drawRoom(stage = 0) {
    ctx.setTransform(2, 0, 0, 2, 0, 0);
    ctx.imageSmoothingEnabled = false;
    rect(0, 0, 480, 300, colors.wall);
    rect(0, 0, 480, 11, '#243b34');
    rect(0, 11, 480, 3, '#6b785b');
    rect(0, 14, 480, 3, '#263c32');
    rect(0, 17, 480, 8, '#3b5447');
    rect(0, 25, 480, 2, '#4b6350');
    rect(0, 27, 480, 173, colors.wall);
    grain(0, 28, 480, 170, 340, ['#3b554b', '#304a43', '#40594c'], 6);
    for (let x = 14; x < 480; x += 26) {
      rect(x, 28, 1, 151, '#3e584c');
      for (let y = 39; y < 170; y += 29) {
        rect(x + 11, y, 1, 5, '#506450');
        rect(x + 9, y + 2, 5, 1, '#506450');
      }
    }
    rect(0, 181, 480, 4, '#8a8660');
    rect(0, 185, 480, 4, '#293e31');
    rect(0, 189, 480, 39, '#2c4438');
    for (let x = 8; x < 480; x += 29) {
      rect(x, 190, 2, 35, '#4b5d42');
      rect(x + 3, 194, 22, 28, '#263c31');
      rect(x + 5, 196, 19, 25, '#324a3a');
    }
    rect(0, 226, 480, 5, '#172d25');
    rect(0, 230, 480, 4, '#8b7752');

    // Staggered oak floorboards and a woven carpet.
    rect(0, 234, 480, 66, '#776748');
    for (let y = 236; y < 300; y += 13) {
      rect(0, y, 480, 1, '#4b4b34');
      rect(0, y + 1, 480, 1, '#97835a');
      for (let x = (y % 2) * 35 - 25; x < 480; x += 79) {
        rect(x, y, 1, 13, '#464d35');
        rect(x + 5, y + 6, 30, 1, '#81704c');
        rect(x + 18, y + 9, 37, 1, '#6c6043');
      }
    }
    polygon([[172, 229], [350, 229], [408, 291], [120, 291]], '#3d4433');
    polygon([[178, 232], [344, 232], [392, 285], [136, 285]], '#a68b5e');
    polygon([[181, 236], [340, 236], [379, 280], [149, 280]], '#486657');
    polygon([[189, 240], [332, 240], [365, 275], [162, 275]], '#b39d6c');
    polygon([[193, 242], [328, 242], [355, 272], [172, 272]], '#4c6957');
    for (let y = 247; y <= 266; y += 9) {
      for (let x = 196; x < 335; x += 21) {
        polygon([[x, y - 3], [x + 4, y], [x, y + 3], [x - 4, y]], '#a28c5c');
        rect(x, y, 1, 1, '#d3b47b');
      }
    }
    for (let x = 141; x < 393; x += 7) rect(x, 286, 2, 4, '#c3a772');

    // Bookcase on the left: several shelves, worn book spines and a small clock.
    rect(12, 47, 119, 125, '#26382c');
    rect(16, 41, 111, 125, '#675c3c');
    rect(20, 46, 102, 115, '#253c32');
    rect(16, 41, 111, 5, '#a08b59');
    rect(16, 46, 4, 116, '#857044');
    rect(121, 45, 6, 119, '#4b4c32');
    rect(22, 50, 98, 1, '#182e27');
    const bookColors = ['#a78451', '#6c8671', '#9c6349', '#b8a77b', '#3f6357', '#7b7660'];
    for (let shelf = 0; shelf < 3; shelf++) {
      const base = 81 + shelf * 37;
      let x      = 25;
      for (let b = 0; b < 10; b++) {
        const width  = 5 + (b * 3 + shelf) % 5;
        const height = 20 + (b * 7 + shelf * 3) % 10;
        book(x, base - height, width, height, bookColors[(b + shelf * 2) % bookColors.length], b === 8);
        x += width + 2;
        if (x > 113) break;
      }
      rect(18, base + 1, 107, 4, '#9e8553');
      rect(19, base + 5, 105, 3, '#3c4933');
    }
    rect(48, 32, 28, 9, '#4b5740');
    rect(52, 23, 20, 18, '#766849');
    rect(55, 25, 14, 13, '#cfbf8a');
    rect(61, 27, 1, 7, '#4a543a');
    rect(61, 32, 4, 1, '#4a543a');

    // Rainy blue window, skyline, moon and heavy green curtains.
    rect(151, 39, 123, 116, '#213c33');
    rect(155, 42, 116, 110, '#b0a574');
    rect(160, 47, 106, 99, '#40636a');
    rect(162, 49, 102, 44, '#385966');
    rect(162, 93, 102, 51, '#527e7e');
    rect(243, 57, 10, 2, '#c2d3ae');
    rect(239, 59, 14, 10, '#c2d3ae');
    rect(243, 69, 10, 2, '#c2d3ae');
    rect(245, 57, 10, 10, '#385966');
    const skyline = [[163, 117, 15, 27], [180, 104, 17, 40], [200, 121, 12, 23], [215, 110, 22, 34], [240, 125, 23, 19]];
    skyline.forEach(([x, y, w, h]) => {
      rect(x, y, w, h, '#3c6669');
      rect(x + 3, y + 7, 2, 3, '#a2b79a');
      rect(x + w - 5, y + 15, 2, 3, '#8ca992');
    });
    for (let i = 0; i < 20; i++) {
      const x = 164 + (i * 19) % 97;
      const y = 52 + (i * 17) % 85;
      rect(x, y, 1, 4, '#98b5a078');
      rect(x - 1, y + 4, 1, 2, '#98b5a078');
    }
    rect(209, 47, 5, 98, '#afa477');
    rect(160, 91, 106, 5, '#afa477');
    rect(214, 48, 2, 96, '#243f37');
    rect(160, 96, 106, 2, '#253f36');
    rect(151, 146, 123, 6, '#c0ad7b');
    rect(147, 152, 131, 4, '#55684b');
    rect(142, 35, 139, 3, '#b7a06b');
    rect(145, 38, 27, 110, '#466654');
    polygon([[145, 39], [173, 39], [169, 101], [160, 122], [161, 150], [144, 150]], '#547459');
    rect(149, 40, 4, 104, '#658365');
    rect(160, 40, 4, 76, '#3f634f');
    rect(145, 119, 18, 4, '#b69b60');
    polygon([[254, 39], [278, 39], [278, 151], [260, 151], [263, 120], [257, 100]], '#466b53');
    rect(270, 40, 4, 106, '#648366');
    rect(261, 119, 17, 4, '#b69b60');
    plant(186, 143, .45);
    rect(224, 142, 22, 4, '#a58350');
    rect(227, 139, 17, 3, '#d0c49b');

    // Wall panel with the exact 3 × 3 affordance from the clue.
    rect(291, 77, 62, 83, '#273b2d');
    rect(288, 75, 62, 78, '#a89662');
    rect(291, 78, 56, 72, '#4e6250');
    rect(294, 81, 50, 9, '#283f31');
    rect(305, 84, 28, 2, '#b1bd8a');
    rect(294, 93, 50, 51, '#263e31');
    for (let row = 0; row < 3; row++) {
      for (let col = 0; col < 3; col++) {
        const x = 297 + col * 15;
        const y = 96 + row * 15;
        rect(x, y, 12, 12, stage >= 2 ? '#85956b' : '#bcb185');
        rect(x + 2, y + 2, 8, 8, '#2b4435');
        rect(x + 5, y + 4, 2, 4, stage >= 2 ? '#c9d7a3' : '#b4b88d');
      }
    }
    rect(314, 148, 9, 2, stage >= 2 ? '#b8d397' : '#d19e61');
    if (stage >= 2) {
      rect(292, 149, 53, 11, '#20372b');
      rect(296, 151, 44, 6, '#3e5540');
      rect(310, 156, 17, 3, '#b9aa73');
    }

    // Door, carved panels, brass lock, lintel, and exit plaque.
    rect(369, 65, 97, 172, '#1b3128');
    rect(373, 67, 87, 164, '#9b8756');
    rect(377, 71, 79, 160, '#4c5e43');
    rect(380, 74, 73, 155, '#516d50');
    if (stage >= 3) {
      rect(380, 74, 73, 155, '#d6d8ab');
      rect(383, 77, 67, 152, '#e8e5bd');
      polygon([[380, 74], [403, 84], [403, 222], [380, 229]], '#456346');
      rect(397, 149, 3, 10, '#c6aa6b');
      polygon([[381, 234], [453, 234], [480, 287], [330, 287]], '#c5c18a66');
    } else {
      rect(386, 81, 60, 58, '#8d8d5e');
      rect(389, 84, 54, 51, '#39543f');
      rect(392, 87, 48, 45, '#48694d');
      rect(391, 89, 2, 40, '#567950');
      rect(386, 161, 60, 57, '#8d8d5e');
      rect(389, 164, 54, 50, '#39543f');
      rect(392, 167, 48, 44, '#48694d');
      rect(391, 169, 2, 38, '#567950');
      rect(434, 143, 8, 14, '#bd9f5b');
      rect(437, 148, 2, 5, '#3a4a32');
      rect(430, 145, 12, 3, '#dfc380');
      rect(377, 71, 3, 157, '#b09b64');
      rect(453, 71, 3, 158, '#2b4734');
    }
    rect(369, 65, 96, 5, '#c1aa73');
    rect(374, 231, 88, 4, '#baa26d');
    rect(398, 48, 40, 13, '#998756');
    rect(400, 50, 36, 9, '#253d30');
    ctx.fillStyle = '#cec492';
    ctx.font      = '6px monospace';
    ctx.fillText('EXIT', 411, 57);

    // Frame and botanical print above the chair.
    rect(280, 27, 30, 32, '#1e352a');
    rect(278, 25, 30, 32, '#ac9160');
    rect(281, 28, 24, 26, '#d6c99a');
    rect(292, 34, 1, 16, '#748059');
    rect(287, 36, 5, 3, '#839064');
    rect(293, 39, 5, 4, '#839064');
    rect(287, 44, 5, 3, '#6b7e53');
    // Hanging wall light and its stepped pool of warm light.
    rect(349, 30, 3, 19, '#b8a56e');
    rect(342, 47, 18, 3, '#c8b27b');
    rect(345, 44, 12, 6, '#d6bf7f');
    rect(347, 50, 7, 3, '#e5d29a');
    polygon([[345, 53], [357, 53], [371, 83], [331, 83]], '#d6bd6c0c');

    // Writing desk stays clear of the held note in the left foreground.
    ctx.save();
    ctx.translate(114, 0);
    rect(35, 240, 133, 7, '#26382a88');
    rect(39, 179, 120, 56, '#675c3e');
    rect(43, 184, 112, 47, '#3a4330');
    rect(46, 187, 106, 31, '#997a4d');
    rect(49, 190, 100, 24, '#776440');
    rect(50, 190, 98, 2, '#ae8b55');
    rect(86, 198, 27, 8, '#3d4630');
    for (let i = 0; i < 4; i++) rect(89 + i * 6, 200, 3, 4, '#d3bd81');
    rect(45, 222, 110, 4, '#8c7047');
    rect(43, 229, 7, 17, '#514b32');
    rect(147, 229, 7, 17, '#514b32');
    rect(43, 229, 3, 16, '#9a7c4c');
    rect(147, 229, 3, 16, '#9a7c4c');
    rect(34, 174, 130, 7, '#ad8a55');
    rect(34, 181, 130, 3, '#464b32');
    rect(37, 172, 124, 2, '#c1a16a');
    if (stage >= 1) {
      rect(46, 187, 106, 28, '#26382b');
      polygon([[46, 208], [152, 208], [158, 224], [40, 224]], '#ae8b54');
      rect(41, 224, 117, 14, '#8e7145');
      rect(43, 225, 112, 2, '#c1a06a');
      rect(91, 229, 18, 4, '#d0b878');
    }
    polygon([[53, 134], [76, 134], [103, 173], [31, 173]], '#d9c57813');
    rect(64, 135, 3, 34, '#b6a367');
    rect(61, 139, 3, 24, '#8d8854');
    rect(55, 168, 22, 4, '#c9b37a');
    rect(58, 165, 16, 4, '#ad995f');
    polygon([[56, 114], [74, 114], [84, 137], [46, 137]], '#b4ad72');
    rect(54, 118, 3, 15, '#cec28a');
    rect(46, 136, 38, 3, '#d9c78b');
    rect(57, 139, 16, 2, '#e6d7a2');
    rect(97, 167, 32, 5, '#3c5f4c');
    rect(99, 168, 28, 2, '#d6c79b');
    rect(93, 162, 30, 5, '#a88553');
    rect(95, 163, 26, 2, '#e3d4a5');
    rect(134, 162, 9, 10, '#b9c0a1');
    rect(143, 164, 3, 5, '#b9c0a1');
    rect(135, 160, 7, 2, '#d5d5b6');

    ctx.restore();

    // Reading chair in the left corner; the desk occupies the window side.
    ctx.save();
    ctx.translate(-151, 0);
    rect(199, 211, 79, 30, '#263c2c88');
    rect(207, 160, 58, 60, '#273f33');
    rect(210, 157, 52, 64, '#8d7952');
    rect(215, 160, 42, 55, '#b39966');
    rect(216, 163, 40, 43, '#bca36e');
    rect(220, 165, 2, 39, '#ccaf77');
    rect(233, 174, 3, 3, '#8e8056');
    rect(246, 174, 3, 3, '#8e8056');
    rect(233, 190, 3, 3, '#8e8056');
    rect(246, 190, 3, 3, '#8e8056');
    rect(203, 199, 62, 8, '#ccb17b');
    rect(203, 207, 62, 21, '#9f895b');
    rect(199, 194, 10, 36, '#8b7953');
    rect(199, 194, 10, 5, '#bea26e');
    rect(260, 194, 10, 36, '#8b7953');
    rect(260, 194, 10, 5, '#bea26e');
    rect(204, 229, 5, 12, '#574f35');
    rect(260, 229, 5, 12, '#574f35');
    polygon([[218, 187], [237, 184], [242, 204], [221, 207]], '#577664');
    rect(222, 189, 3, 12, '#75917a');
    ctx.restore();
    rect(274, 220, 41, 4, '#b09865');
    rect(277, 224, 35, 4, '#675f40');
    rect(280, 227, 3, 19, '#a18b58');
    rect(306, 227, 3, 19, '#a18b58');
    rect(283, 215, 24, 5, '#9e684a');
    rect(285, 216, 20, 2, '#d7cba1');
    plant(345, 222, .84);
    rect(330, 242, 30, 3, '#2c4030');
    // Near foreground soft shadows and a low crate of rolled papers.
    rect(5, 263, 52, 27, '#6f6443');
    rect(5, 263, 52, 4, '#9a8555');
    rect(9, 270, 45, 2, '#504d32');
    rect(9, 281, 45, 2, '#504d32');
    rect(14, 248, 8, 16, '#cbbd90');
    rect(25, 243, 7, 21, '#e0d0a0');
    rect(37, 252, 9, 12, '#b1b290');
    rect(18, 250, 1, 10, '#9b9672');
    rect(29, 245, 1, 17, '#b2a681');
    rect(0, 294, 480, 6, '#20382d55');
  }
  window.drawRoom = drawRoom;
})();
