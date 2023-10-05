package com.example.AmigoBot.service;

import com.example.AmigoBot.config.BotConfig;
import com.example.AmigoBot.model.Ads;
import com.example.AmigoBot.model.AdsRepository;
import com.example.AmigoBot.model.User;
import com.example.AmigoBot.model.UserRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.vdurmont.emoji.EmojiParser;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.AnswerCallbackQuery;
import org.telegram.telegrambots.meta.api.methods.commands.SetMyCommands;
import org.telegram.telegrambots.meta.api.methods.send.SendLocation;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageText;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.commands.BotCommand;
import org.telegram.telegrambots.meta.api.objects.commands.scope.BotCommandScopeDefault;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboard;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardRow;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.ProtocolException;
import java.net.URL;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Slf4j
@Component
public class TelegramBot extends TelegramLongPollingBot {
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private AdsRepository adsRepository;
    final BotConfig config;
    final String HELP_TEXT = "This bot is absolutely useless at the moment\n";
    static final String YES_BUTTON = "YES_BUTTON";
    static final String NO_BUTTON = "NO_BUTTON";
    static final String ERROR_TEXT = "Error occured: ";

    public TelegramBot(BotConfig config) {
        this.config = config;
        List<BotCommand> listOfCommands = new ArrayList<>();
        listOfCommands.add(new BotCommand("/start", "Welcome message"));
        listOfCommands.add(new BotCommand("/mydata", "User detailed information"));
        listOfCommands.add(new BotCommand("/deletedata", "Delete my data"));
        listOfCommands.add(new BotCommand("/help", "Info how to use this bot"));
        listOfCommands.add(new BotCommand("/settings", "Set your preferences"));
        try {
            this.execute(new SetMyCommands(listOfCommands, new BotCommandScopeDefault(), null));
        } catch (TelegramApiException e) {
            log.error("Error setting bots command list: " + e.getMessage());
        }
    }

    @Override
    public String getBotToken() {
        return config.getToken();
    }

    @Override
    public void onUpdateReceived(Update update) {
        if (update.hasMessage() && update.getMessage().hasText()) {
            String reply = update.getMessage().getText();
            Message message = update.getMessage();
            long chatId = update.getMessage().getChatId();
            if (reply.startsWith("/")) {
                commandHandler(reply, chatId, message);
            } else {

            }
        }
    }

    public void commandHandler(String messageText, long chatId, Message message) {
        if (messageText.contains("/send") && config.getOwnerId() == chatId) {
            var textToSend = EmojiParser.parseToUnicode(messageText.substring(messageText.indexOf(" ")));
            var users = userRepository.findAll();
            for (User user : users) {
                prepareAndSendMessage(user.getChatId(), textToSend);
            }

        } else {
            if (messageText.equals("/start")) {
                registerUser(message);
                startCommandReceived(chatId, message.getChat().getFirstName());
            } else if (messageText.equals("/help")) {
                prepareAndSendMessage(chatId, HELP_TEXT);
            } else if (messageText.equals("/register")) {
                register(chatId);
            } else if (messageText.contains("/weather")) {
                String str = "Что-то пошло не так";
                String city = null;
                String [] array = messageText.split(" ");
                if (array.length==2){
                    city = array[1];
                }
                try {
                    str = prepareWeatherForecast(city);
                } catch (Exception e) {
                    log.error(ERROR_TEXT + e.getMessage());
                }
                prepareAndSendMessage(chatId, str);
            } else {
                prepareAndSendMessage(chatId, "Я еще этого не умею :(");
            }

        }
    }

    //    @Override
//    public void onUpdateReceived(Update update) {
//        if (update.hasMessage() && update.getMessage().hasText()) {
//            String messageText = update.getMessage().getText();
//            long chatId = update.getMessage().getChatId();
//
//            if (messageText.contains("/send") && config.getOwnerId() == chatId) {
//                var textToSend = EmojiParser.parseToUnicode(messageText.substring(messageText.indexOf(" ")));
//                var users = userRepository.findAll();
//                for (User user : users) {
//                    prepareAndSendMessage(user.getChatId(), textToSend);
//                }
//
//            } else {
//                switch (messageText) {
//                    case "/start":
//                        registerUser(update.getMessage());
//                        startCommandReceived(chatId, update.getMessage().getChat().getFirstName());
//                        break;
//                    case "/help":
//                        prepareAndSendMessage(chatId, HELP_TEXT);
//                        break;
//                    case "/register":
//                        register(chatId);
//                        break;
//                    case "/weather":
//
//                        //------------------------------------------
//                        String city = "Minsk";
//                        String message = "Что-то пошло не так";
//                        try {
//                            message = prepareWeatherForecast(city);
//                        } catch (Exception e) {
//                            log.error(ERROR_TEXT + e.getMessage());
//                        }
//                        prepareAndSendMessage(chatId,message);
//                        break;
//                        //------------------------------------------------
//                    default:
//                        prepareAndSendMessage(chatId, "Я еще этого не умею :(");
//                }
//
//            }
//
//        } else if (update.hasCallbackQuery()) {
//            String callBackData = update.getCallbackQuery().getData();
//            long messageId = update.getCallbackQuery().getMessage().getMessageId();
//            long chatId = update.getCallbackQuery().getMessage().getChatId();
//            if (callBackData.equals(YES_BUTTON)) {
//                String text = "You pressed YES button";
//                executeEditMessageText(text, chatId, messageId);
//            } else if (callBackData.equals(NO_BUTTON)) {
//                String text = "You pressed NO button";
//                executeEditMessageText(text, chatId, messageId);
//            }
//        }
//    }
    private void register(long chatId) {
        SendMessage message = new SendMessage();
        message.setChatId(String.valueOf(chatId));
        message.setText("Do you really want to register?");
        InlineKeyboardMarkup markupInLine = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> rowsInLine = new ArrayList<>();
        List<InlineKeyboardButton> rowInLine = new ArrayList<>();
        var yesButton = new InlineKeyboardButton();
        yesButton.setText("Yes");
        yesButton.setCallbackData("YES_BUTTON");
        var noButton = new InlineKeyboardButton();
        noButton.setText("No");
        noButton.setCallbackData("NO_BUTTON");
        rowInLine.add(yesButton);
        rowInLine.add(noButton);
        rowsInLine.add(rowInLine);
        markupInLine.setKeyboard(rowsInLine);
        message.setReplyMarkup(markupInLine);
        executeMessage(message);
    }

    private void registerUser(Message message) {
        if (userRepository.findById(message.getChatId()).isEmpty()) {
            var chatId = message.getChatId();
            var chat = message.getChat();
            User user = new User();
            user.setChatId(chatId);
            user.setFirstName(chat.getFirstName());
            user.setLastName(chat.getLastName());
            user.setUserName(chat.getUserName());
            user.setFirstName(chat.getFirstName());
            user.setRegisteredAt(new Timestamp(System.currentTimeMillis()));

            userRepository.save(user);
            log.info("User saved " + user);
        }
    }

    private void startCommandReceived(long chatId, String name) {
        String answer = EmojiParser.parseToUnicode("Привет, " + name + ", как я могу тебе помочь?" + " :blush:");
        //String answer = "Привет, "+name + ", как я могу тебе помочь?";
        log.info("Replied to user " + name);
        sendMessage(chatId, answer);
    }


    private void sendMessage(long chatId, String textToSend) {
        SendMessage message = new SendMessage();
        message.setChatId(String.valueOf(chatId));
        message.setText(textToSend);

        //------------------------------------------------------Добавление кнопок
        message.setReplyMarkup(getKeybordButtons());

        //------------------------------------------------------
        executeMessage(message);
    }

    public ReplyKeyboardMarkup getKeybordButtons() {
        ReplyKeyboardMarkup keyboardMarkup = new ReplyKeyboardMarkup();
        List<KeyboardRow> keyboardRows = new ArrayList<>();
        KeyboardRow row = new KeyboardRow();
        row.add("/weather");
        row.add("get random joke");

        keyboardRows.add(row);
        row = new KeyboardRow();
        row.add("register");
        row.add("check my data");
        row.add("delete my data");
        keyboardRows.add(row);
        keyboardMarkup.setKeyboard(keyboardRows);
        return keyboardMarkup;
    }

    private void executeEditMessageText(String text, long chatId, long messageId) {
        EditMessageText message = new EditMessageText();
        message.setChatId(String.valueOf(chatId));
        message.setText(text);
        message.setMessageId((int) messageId);
        try {
            execute(message);
        } catch (TelegramApiException e) {
            log.error(ERROR_TEXT + e.getMessage());
        }


    }

    private void executeMessage(SendMessage message) {
        try {
            execute(message);
        } catch (TelegramApiException e) {
            log.error(ERROR_TEXT + e.getMessage());
        }
    }

    private void prepareAndSendMessage(long chatId, String textToSend) {
        SendMessage message = new SendMessage();
        message.setChatId(String.valueOf(chatId));
        message.setText(textToSend);
        executeMessage(message);
    }

    //    @Scheduled (cron = "${cron.scheduler}")
//    private void sendAds() {
//        Iterable <Ads> ads = adsRepository.findAll();
//        Iterable<User> users = userRepository.findAll();
//        for (Ads ad:ads) {
//            for (User user: users){
//                prepareAndSendMessage(user.getChatId(), ad.getAd());
//            }
//        }
//
//    }
    private String prepareWeatherForecast(String city) throws IOException {
        final DateTimeFormatter INPUT_DATE_TIME_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        final DateTimeFormatter OUTPUT_DATE_TIME_FORMAT = DateTimeFormatter.ofPattern("EEEE dd MMMM HH:mm", Locale.US);
        Map<String, String> map = new HashMap<>();
        map.put("clear sky", "Солнечно 🌞️");
        map.put("scattered clouds", "Облачно 🌥️");
        map.put("few clouds", "Малооблачно 🌤️");
        map.put("overcast clouds", "Пасмурно ⛅");
        map.put("broken clouds", "Пасмурно ⛅");
        map.put("light rain", "Лёгкий дождь 🌦️");


        //--------------------------------------------------------------------------------------
        // Создание HTTP запроса
        final String API_CALL_TEMPLATE = "https://api.openweathermap.org/data/2.5/forecast?q=";
        final String API_KEY_TEMPLATE = "&APPID=658841465c89239aed6eef0c23849bc4";
        StringBuffer stringBuffer = new StringBuffer();
        String urlString = API_CALL_TEMPLATE + city + API_KEY_TEMPLATE;
        URL urlObject = new URL(urlString);
        HttpURLConnection connection = (HttpURLConnection) urlObject.openConnection();
        connection.setRequestMethod("GET");
        connection.setRequestProperty("User-Agent", "Mozilla/5.0");
        // connection.getContentType();
        int responseCode = connection.getResponseCode();
        if (responseCode == 404) {
            throw new IllegalArgumentException();
        }
        BufferedReader in = new BufferedReader(new InputStreamReader(connection.getInputStream()));
        String inputLine;
        StringBuffer response = new StringBuffer();
        while ((inputLine = in.readLine()) != null) {
            response.append(inputLine);
        }
        in.close();
        //--------------------------------------------------------------------------------------
        // Обработка JSON ответа от сервера
        String data = response.toString();
        String cityForForecast = new ObjectMapper().readTree(data).get("city").get("name").toString();
        JsonNode arrNode = new ObjectMapper().readTree(data).get("list");
        StringBuilder sb = new StringBuilder();
        sb.append("Погода на 5 дней для города "+cityForForecast+":\n");

        for (JsonNode node : arrNode) {
            String str = node.get("dt_txt").toString();
            if (str.contains("09:00:00") || str.contains("15:00:00") || str.contains("21:00:00")) {
                String date = node.get("dt_txt").toString().replaceAll("\"", "");
                LocalDateTime localDateTime = LocalDateTime.parse(date, INPUT_DATE_TIME_FORMAT);
                sb.append(localDateTime.getDayOfMonth() + " ");
                sb.append(Month.valueOf(localDateTime.getMonth().toString()).getName() + ", ");
                sb.append(DayOfWeek.valueOf(localDateTime.getDayOfWeek().toString()).getName() + ", ");
                sb.append("Время " + localDateTime.getHour() + " часов,");
                int temperature = (int) Math.round((node.get("main").get("temp").asDouble() - 273.15));
                sb.append(" Температура: " + temperature + "℃,");
                String description = node.get("weather").get(0).get("description").toString();
                System.out.println(description);
                description = description.replaceAll("\"", "");
                sb.append(" " + map.get(description));
                sb.append("\n");
            }
        }
        return sb.toString();
    }


    @Override
    public String getBotUsername() {
        return config.getBotName();
    }

}
