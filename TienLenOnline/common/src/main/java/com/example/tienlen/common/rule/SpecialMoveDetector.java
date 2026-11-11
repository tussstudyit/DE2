package com.example.tienlen.common.rule;

import com.example.tienlen.common.model.Card;
import java.util.List;
import java.util.Random;

/**
 * Nhận diện các sự kiện chặt bài, chặn bài đặc biệt (chặt Heo, chặt hàng, tứ quý, 3 đôi thông, 4 đôi thông)
 * và tạo các thông báo hài hước, vui nhộn, đậm chất văn hóa bài Tiến Lên Miền Nam.
 */
public class SpecialMoveDetector {
    private static final Random RANDOM = new Random();

    /**
     * Phân tích nước đánh của attacker đè lên defenderCards và trả về thông báo vui nhộn nếu có sự kiện chặt bài/đặc biệt.
     * @return Chuỗi thông báo hài hước kèm emoji hoặc null nếu là nước đánh thông thường.
     */
    public static String detectSpecialEvent(String attackerName, List<Card> attackerCards, String defenderName, List<Card> defenderCards) {
        if (attackerCards == null || attackerCards.isEmpty()) {
            return null;
        }

        CardCombinationType attackerType = TienLenRule.getCombinationType(attackerCards);

        // Trường hợp bàn trống hoặc người đánh đầu vòng với bộ bài đặc biệt
        if (defenderCards == null || defenderCards.isEmpty()) {
            return switch (attackerType) {
                case FOUR_CONSECUTIVE_PAIRS -> getRandom(
                        "🌪️👑 SIÊU PHẨM 4 ĐÔI THÔNG! " + attackerName + " tung đòn hủy diệt, cả bàn cẩn thận! 🔥",
                        "⚡ 4 ĐÔI THÔNG TRÌNH LÀNG! Sức mạnh tối thượng từ " + attackerName + "! Ai dám cản?!"
                );
                case FOUR_OF_A_KIND -> getRandom(
                        "💣🔥 TỨ QUÝ XUẤT TRẬN! " + attackerName + " nã đại bác khiến cả phòng khiếp sợ! 😱",
                        "💥 BÙM! TỨ QUÝ của " + attackerName + " giáng xuống bàn đấu! Chuẩn bị tinh thần đi!"
                );
                case THREE_CONSECUTIVE_PAIRS -> getRandom(
                        "⚔️✨ 3 ĐÔI THÔNG RA ĐẬU! " + attackerName + " múa kiếm cực kỳ hiểm hóc! ⚡",
                        "🔥 3 ĐÔI THÔNG KHỞI SỰ! " + attackerName + " rình mồi cực khét!"
                );
                default -> null;
            };
        }

        CardCombinationType defenderType = TienLenRule.getCombinationType(defenderCards);
        boolean defenderHasHeo = defenderCards.stream().anyMatch(Card::isHeo);

        // 1. Chặt 1 con Heo (Heo đen hoặc Heo đỏ)
        if (defenderType == CardCombinationType.SINGLE && defenderHasHeo) {
            boolean isRedHeo = defenderCards.get(0).isRedHeo();
            String heoTypeStr = isRedHeo ? "HEO ĐỎ BÉO NGẬY" : "HEO ĐEN";

            if (attackerType == CardCombinationType.THREE_CONSECUTIVE_PAIRS) {
                return getRandom(
                        "🐷🔨 ỐI GIỒI ÔI! " + attackerName + " vung 3 ĐÔI THÔNG CHẶT ĐẸP " + heoTypeStr + " của " + defenderName + "! Đau như hoạn! 🍖😂",
                        "💥 BÙM CHÍU! " + attackerName + " xách 3 ĐÔI THÔNG chém rụng " + heoTypeStr + " của " + defenderName + "! Cay đỏ mắt! 🌶️🤣",
                        "🐷👋 VĨNH BIỆT HEO CƯNG! " + defenderName + " vừa thả Heo đã bị " + attackerName + " làm thịt bằng 3 ĐÔI THÔNG! Khóc ròng! 😭"
                );
            }
            if (attackerType == CardCombinationType.FOUR_OF_A_KIND) {
                return getRandom(
                        "💣💥 TỨ QUÝ CHẶT HEO! " + attackerName + " nện TỨ QUÝ đè nát " + heoTypeStr + " của " + defenderName + "! Rung chuyển sới bài! 🐷💨",
                        "🐷🔥 HEO QUAY NGUYÊN CON! " + attackerName + " xách TỨ QUÝ tiễn Heo của " + defenderName + " về miền cực lạc! Xin vĩnh biệt! ⚰️😂",
                        "🔨💸 ĐAU HƠN BỊ BỒ ĐÁ! " + defenderName + " rớt " + heoTypeStr + " vào tay TỨ QUÝ của " + attackerName + "! Tiền bay theo gió! 💸"
                );
            }
            if (attackerType == CardCombinationType.FOUR_CONSECUTIVE_PAIRS) {
                return getRandom(
                        "🌪️👑 SIÊU BOM NGUYÊN TỬ! 4 ĐÔI THÔNG của " + attackerName + " nghiền nát " + heoTypeStr + " của " + defenderName + " không thương tiếc! 💥🐷",
                        "⚡ CÚ CHÉM HUYỀN THOẠI! " + attackerName + " tung 4 ĐÔI THÔNG hủy diệt " + heoTypeStr + " của " + defenderName + "! Quá tàn nhẫn! 🙇‍♂️🔥"
                );
            }
        }

        // 2. Chặt Đôi Heo
        if (defenderType == CardCombinationType.PAIR && defenderHasHeo) {
            if (attackerType == CardCombinationType.FOUR_OF_A_KIND) {
                return getRandom(
                        "🐷🐷💣 ĐÔI HEO BỐC HƠI! " + attackerName + " tung TỨ QUÝ đè bẹp ĐÔI HEO của " + defenderName + "! Cả bàn nín thở rùng mình! 😱",
                        "💥 CÚ ĐẬP KINH HOÀNG! ĐÔI HEO béo của " + defenderName + " bị TỨ QUÝ của " + attackerName + " hốt trọn! Khóc một dòng sông! 😭💸",
                        "🐷🐷🔥 NƯỚNG TRỌN ĐÔI HEO! TỨ QUÝ của " + attackerName + " tiễn ĐÔI HEO của " + defenderName + " lên đĩa! Cay xè sống mũi! 🌶️😂"
                );
            }
            if (attackerType == CardCombinationType.FOUR_CONSECUTIVE_PAIRS) {
                return getRandom(
                        "🌪️⚡ THẢM HỌA ĐÔI HEO! 4 ĐÔI THÔNG của " + attackerName + " thổi bay ĐÔI HEO của " + defenderName + " ra rìa vũ trụ! 🚀👑",
                        "👑🔥 ĐỈNH NÓC KỊCH TRẦN! " + attackerName + " xòe 4 ĐÔI THÔNG chém đẹp ĐÔI HEO của " + defenderName + "! Bay phấp phới luôn! 🎉"
                );
            }
        }

        // 3. Chặt 3 đôi thông
        if (defenderType == CardCombinationType.THREE_CONSECUTIVE_PAIRS) {
            if (attackerType == CardCombinationType.THREE_CONSECUTIVE_PAIRS) {
                return getRandom(
                        "⚔️🔥 3 ĐÔI THÔNG ĐÈ 3 ĐÔI THÔNG! Vỏ quýt dày có móng tay nhọn! " + attackerName + " lật kèo " + defenderName + " cực gắt! 😱",
                        "💥 TRANH HÙNG ĐÔI THÔNG! " + attackerName + " tung 3 đôi thông đè bẹp " + defenderName + "! Đau thấu tâm can! 😭"
                );
            }
            if (attackerType == CardCombinationType.FOUR_OF_A_KIND) {
                return getRandom(
                        "💣⚡ TỨ QUÝ BẺ GÃY 3 ĐÔI THÔNG! " + attackerName + " phản đòn rực lửa, " + defenderName + " ngậm đắng nuốt cay! 🌶️😂",
                        "🔥 LẤY THỊT ĐÈ NGƯỜI! TỨ QUÝ của " + attackerName + " nghiền nát 3 đôi thông của " + defenderName + "! Cười ra nước mắt! 😂"
                );
            }
            if (attackerType == CardCombinationType.FOUR_CONSECUTIVE_PAIRS) {
                return getRandom(
                        "👑🌪️ 4 ĐÔI THÔNG NUỐT CHỬNG 3 ĐÔI THÔNG! Đẳng cấp tối thượng từ " + attackerName + "! Không còn đường lùi! 🚀"
                );
            }
        }

        // 4. Chặt Tứ quý
        if (defenderType == CardCombinationType.FOUR_OF_A_KIND) {
            if (attackerType == CardCombinationType.FOUR_OF_A_KIND) {
                return getRandom(
                        "😱💥 TỨ QUÝ ĐÈ TỨ QUÝ! CÚ LẬT KÈO THẾ KỶ! " + attackerName + " đè bẹp Tứ quý của " + defenderName + "! Thần tài gõ cửa rồi! 💰🎉",
                        "💣💣 ĐẠI CHIẾN TỨ QUÝ! " + defenderName + " vừa cười hớn hở thì bị " + attackerName + " nện Tứ quý to hơn đè sấp mặt! Cười người hôm trước hôm sau người cười! 😂"
                );
            }
            if (attackerType == CardCombinationType.FOUR_CONSECUTIVE_PAIRS) {
                return getRandom(
                        "👑💣 4 ĐÔI THÔNG CHẶT TỨ QUÝ! Cú knock-out lịch sử! " + attackerName + " tiễn Tứ quý của " + defenderName + " vào dĩ vãng! Quá kinh điển! 🏆🔥"
                );
            }
        }

        // 5. Chặt 4 đôi thông
        if (defenderType == CardCombinationType.FOUR_CONSECUTIVE_PAIRS && attackerType == CardCombinationType.FOUR_CONSECUTIVE_PAIRS) {
            return getRandom(
                    "🌌⚡ ĐẠI CHIẾN 4 ĐÔI THÔNG VÔ TIỀN KHOÁNG HẬU! " + attackerName + " xuất chiêu trùm cuối hạ gục " + defenderName + "! Ai cứu nổi quả này?! 😱👑"
            );
        }

        return null;
    }

    private static String getRandom(String... options) {
        if (options == null || options.length == 0) return "";
        return options[RANDOM.nextInt(options.length)];
    }
}
