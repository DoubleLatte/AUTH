package me.ddlatte.auth;

import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.entities.Activity;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;

import javax.security.auth.login.LoginException;

public class BOT extends ListenerAdapter {

    public static void main(String[] args) throws LoginException {
        // JDA 인스턴스를 생성하고 봇 토큰으로 로그인합니다.
        JDABuilder jdaBuilder = JDABuilder.createDefault("YOUR_BOT_TOKEN");

        // 봇의 상태 메시지를 설정합니다.
        jdaBuilder.setActivity(Activity.playing("명령어를 기다리는 중..."));

        // 이벤트 리스너를 추가합니다.
        jdaBuilder.addEventListeners(new BOT());

        // JDA를 통해 봇을 시작합니다.
        jdaBuilder.build();
    }

    @Override
    public void onMessageReceived(MessageReceivedEvent event) {
        // 봇이 보낸 메시지는 무시합니다.
        if (event.getAuthor().isBot()) {
            return;
        }

        // 메시지가 "!ping"이라면 응답합니다.
        String message = event.getMessage().getContentRaw();
        if (message.equalsIgnoreCase("!ping")) {
            event.getChannel().sendMessage("Pong!").queue();
        }
    }
}
