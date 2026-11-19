package coderoom.app;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SceneObjectButtonTest {
    @Test void onlyOneActionRunsAfterAllAssetFrames() throws Exception {
        CountDownLatch finished = new CountDownLatch(1);
        AtomicInteger count     = new AtomicInteger();
        SwingUtilities.invokeAndWait(() -> {
            JPanel parent = new JPanel();
            SceneObjectButton button = new SceneObjectButton("방 선택하기", "curtain", 6, () -> {
                count.incrementAndGet();
                finished.countDown();
            });
            parent.add(button);
            button.setSize(510, 600);
            assertFalse(button.contains(0, 0));
            assertTrue(button.contains(255, 300));
            button.doClick(0);
            button.doClick(0);
            assertEquals(0, count.get());
        });
        assertTrue(finished.await(3, TimeUnit.SECONDS));
        assertEquals(1, count.get());
    }

    @Test void removingObjectCancelsPendingNavigation() throws Exception {
        AtomicInteger count = new AtomicInteger();
        SwingUtilities.invokeAndWait(() -> {
            SceneObjectButton button = new SceneObjectButton("방 선택하기", "curtain", 6, count::incrementAndGet);
            button.doClick(0);
            button.removeNotify();
        });
        Thread.sleep(850);
        assertEquals(0, count.get());
    }

    @Test void atlasAndLabelFilesHaveRealTransparency() throws Exception {
        AtomicReference<Throwable> failure = new AtomicReference<>();
        SwingUtilities.invokeAndWait(() -> {
            try {
                for (String name : new String[]{"curtain", "bookshelf", "calendar"}) {
                    var atlas = SceneObjectButton.read(name + ".png");
                    assertTrue(atlas.getColorModel().hasAlpha());
                    assertTrue((atlas.getRGB(0, 0) >>> 24) <= 16, name + " 배경은 투명해야 합니다.");
                    assertEquals(0, atlas.getWidth() % 3);
                    assertEquals(0, atlas.getHeight() % 2);
                }
                assertTrue(SceneObjectButton.read("label-curtain.png").getColorModel().hasAlpha());
            } catch (Throwable exception) {
                failure.set(exception);
            }
        });
        if (failure.get() != null) {
            throw new AssertionError(failure.get());
        }
    }

    @Test void curtainOpeningKeepsBackdropClearAndSurroundStationary() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            SceneObjectButton button = new SceneObjectButton("방 선택하기", "curtain", 6, () -> {});
            var first                = button.artworkFrame(0);
            var moving               = button.movingRegion();
            for (int index = 1; index < 6; index++) {
                var next = button.artworkFrame(index);
                for (int y = 0; y < first.getHeight(); y++) {
                    for (int x = 0; x < first.getWidth(); x++) {
                        if (!moving.contains(x, y)) {
                            assertEquals(first.getRGB(x, y), next.getRGB(x, y), "커튼 주변은 고정되어야 합니다.");
                        }
                    }
                }
                for (int y = 80; y < 300; y++) {
                    assertEquals(0, next.getRGB(140, y) >>> 24, "열린 틈의 잔상이 배경을 흔들면 안 됩니다.");
                }
            }
        });
    }

    @Test void stationaryFurniturePixelsNeverChangeBetweenFrames() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            for (String kind : new String[]{"bookshelf", "calendar"}) {
                SceneObjectButton button = new SceneObjectButton("기록", kind, 6, () -> {});
                var first                = button.artworkFrame(0);
                var moving               = button.movingRegion();
                long changed             = 0;
                for (int frame = 1; frame < 6; frame++) {
                    var next = button.artworkFrame(frame);
                    for (int y = 0; y < first.getHeight(); y++) {
                        for (int x = 0; x < first.getWidth(); x++) {
                            if (first.getRGB(x, y) != next.getRGB(x, y)) {
                                assertTrue(moving.contains(x, y), kind + " 고정 사물이 흔들립니다.");
                                changed++;
                            }
                        }
                    }
                }
                assertTrue(changed > 100, "움직이는 에셋 프레임은 실제로 달라야 합니다.");
            }
        });
    }

    @Test void popupActivationClearsHoverUntilThePointerActuallyMoves() throws Exception {
        for (String kind : new String[]{"leaderboard", "calendar"}) {
            CountDownLatch finished = new CountDownLatch(1);
            AtomicReference<SceneObjectButton> reference = new AtomicReference<>();
            SwingUtilities.invokeAndWait(() -> {
                JPanel parent = new JPanel();
                SceneObjectButton button = new SceneObjectButton("기록", kind, "leaderboard".equals(kind) ? 1 : 6, () -> {
                    // 모달 복귀 시 들어오는 합성 mouse-enter를 재현한다.
                    reference.get().getModel().setRollover(true);
                    assertFalse(reference.get().isHighlighted());
                    finished.countDown();
                });
                reference.set(button);
                parent.add(button);
                button.setSize(300, 400);
                button.pointerHover(true);
                assertTrue(button.isHighlighted());
                button.doClick(0);
                assertFalse(button.isHighlighted());
            });
            assertTrue(finished.await(3, TimeUnit.SECONDS));
            SwingUtilities.invokeAndWait(() -> {
                SceneObjectButton button = reference.get();
                button.getModel().setRollover(true);
                assertFalse(button.isHighlighted());
                button.pointerHover(true);
                assertTrue(button.isHighlighted());
            });
        }
    }

    @Test void bookshelfOpensImmediatelyAndCurtainNeverHighlights() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            AtomicInteger calls       = new AtomicInteger();
            SceneObjectButton books   = new SceneObjectButton("풀이한 문제", "bookshelf", 1, calls::incrementAndGet);
            SceneObjectButton atlas   = new SceneObjectButton("풀이한 문제", "bookshelf", 6, () -> {});
            SceneObjectButton curtain = new SceneObjectButton("방 선택하기", "curtain", 6, () -> {});
            assertEquals(atlas.artworkFrame(0).getWidth(), books.artworkFrame(0).getWidth());
            books.doClick(0);
            assertEquals(1, calls.get());
            curtain.pointerHover(true);
            assertFalse(curtain.isHighlighted());
        });
    }

    @Test void windowLeavesLoopWithoutPaintingOutsideTheGlass() {
        WindowAmbient ambient = new WindowAmbient();
        var image             = new java.awt.image.BufferedImage(512, 512, java.awt.image.BufferedImage.TYPE_INT_ARGB);
        var graphics          = image.createGraphics();
        ambient.paint(graphics, 0, 0, 512, 512);
        graphics.dispose();
        long visible = 0;
        for (int y = 0; y < 512; y++) {
            for (int x = 0; x < 512; x++) {
                if ((image.getRGB(x, y) >>> 24) > 0) {
                    assertTrue(x >= 84 && x < 438 && y >= 148 && y < 472);
                    visible++;
                }
            }
        }
        assertTrue(visible > 100);
        assertFalse(ambient.isFalling());
        for (int i = 0; i < 249; i++) ambient.advance();
        assertFalse(ambient.isFalling());
        ambient.advance();
        assertTrue(ambient.isFalling());
        for (int i = 0; i < 100; i++) ambient.advance();
        assertFalse(ambient.isFalling());
        for (int i = 0; i < 249; i++) ambient.advance();
        assertFalse(ambient.isFalling());
        assertEquals(0, SceneObjectButton.read("window-leaves.png").getRGB(50, 180) >>> 24);
    }

}
