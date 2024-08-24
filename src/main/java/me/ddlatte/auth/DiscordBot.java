package me.ddlatte.auth;

import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.components.buttons.Button;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;

public class DiscordBot extends ListenerAdapter {
    private JDA jda;
    private Map<String, String> verificationCodes = new HashMap<>();

    public void initialize(String token) {
        try {
            jda = JDABuilder.createDefault(token)
                    .addEventListeners(this)
                    .build();
            jda.awaitReady();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onButtonInteraction(ButtonInteractionEvent event) {
        if (event.getComponentId().equals("generate_code")) {
            String code = generateVerificationCode();
            String userId = event.getUser().getId();
            verificationCodes.put(userId, code);
            event.reply("Your verification code is: " + code).setEphemeral(true).queue();
        }
    }

    private String generateVerificationCode() {
        Random random = new Random();
        return String.format("%06d", random.nextInt(1000000));
    }

    public boolean verifyCode(String userId, String code) {
        String storedCode = verificationCodes.get(userId);
        if (storedCode != null && storedCode.equals(code)) {
            verificationCodes.remove(userId);
            return true;
        }
        return false;
    }

    public void shutdown() {
        if (jda != null) {
            jda.shutdown();
        }
    }
}