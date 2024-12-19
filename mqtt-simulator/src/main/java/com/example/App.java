package com.example;

import org.eclipse.paho.client.mqttv3.*;
import java.util.Scanner;

public class App {
    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);

        try {
            // 获取用户输入的 MQTT 服务地址和端口
            System.out.print("请输入 MQTT 服务地址（默认：broker.hivemq.com）：");
            String broker = scanner.nextLine().trim();
            if (broker.isEmpty()) {
                broker = "broker.hivemq.com";
            }

            System.out.print("请输入端口号（默认：1883）：");
            String portInput = scanner.nextLine().trim();
            int port = 1883;
            if (!portInput.isEmpty()) {
                try {
                    port = Integer.parseInt(portInput);
                } catch (NumberFormatException e) {
                    System.out.println("端口号无效，将使用默认端口 1883。");
                }
            }
            broker = "tcp://" + broker + ":" + port;

            System.out.println("连接到的地址：" + broker);

            // 获取主题
            System.out.print("请输入主题：");
            String topic = scanner.nextLine().trim();
            if (topic.isEmpty()) {
                System.out.println("主题不能为空！");
                return;
            }

            // 创建 MQTT 客户端
            String clientId = "JavaMQTTClient" + System.currentTimeMillis(); // 客户端 ID
            MqttClient client = new MqttClient(broker, clientId, null);

            // 设置回调
            client.setCallback(new MqttCallback() {
                @Override
                public void connectionLost(Throwable cause) {
                    System.out.println("连接丢失！" + cause.getMessage());
                }

                @Override
                public void messageArrived(String topic, MqttMessage message) {
                    System.out.println("接收到消息：");
                    System.out.println("\t主题：" + topic);
                    System.out.println("\t内容：" + new String(message.getPayload()));
                }

                @Override
                public void deliveryComplete(IMqttDeliveryToken token) {
                    System.out.println("消息已发送！");
                }
            });

            // 连接 MQTT 服务器
            MqttConnectOptions connOpts = new MqttConnectOptions();
            connOpts.setCleanSession(true);
            System.out.println("正在连接到 MQTT 服务器...");
            client.connect(connOpts);
            System.out.println("连接成功！");

            // 订阅主题
            client.subscribe(topic);
            System.out.println("已订阅主题：" + topic);

            // 启动发送线程
            Thread sendThread = new Thread(() -> {
                try {
                    System.out.println("输入消息内容（输入 'q!' 退出）：");
                    while (true) {
                        String content = scanner.nextLine().trim();
                        if ("q!".equals(content)) {
                            System.out.println("程序退出中...");
                            client.disconnect();
                            System.exit(0);
                        }
                        if (!content.isEmpty()) {
                            MqttMessage message = new MqttMessage(content.getBytes());
                            message.setQos(1); // 服务质量
                            client.publish(topic, message);
                        }
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            });

            sendThread.setDaemon(true);
            sendThread.start();

            // 等待接收消息，主线程空转直到用户退出
            synchronized (App.class) {
                App.class.wait();
            }

        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            scanner.close();
        }
    }
}
