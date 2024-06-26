package Threads;

import javax.imageio.ImageIO;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.SourceDataLine;
import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.*;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetSocketAddress;
import java.net.SocketException;
import java.nio.ByteBuffer;
import java.util.Arrays;

public class UdpReceiveThread implements Runnable {
    private int ID;
    private InetSocketAddress remoteReceiver;
    private AudioFormat format;
    private SourceDataLine sourceDataLine;
    private DatagramSocket udpReceiver;
    private DatagramPacket datagramPacket;
    private int localReceiverPort;//local udp receiver's port
    private InputStream inputStream;
    private DataInputStream dataInputStream;
    private JFrame window;
    private Graphics g;

    public UdpReceiveThread(int localReceiverPort,int ID,InetSocketAddress remoteReceiver) throws IOException {
        this.ID=ID;
        this.remoteReceiver=remoteReceiver;
        this.format = new AudioFormat(22050, 16, 1, true, false);
        System.out.println("----receiving video:  " + localReceiverPort + "----");
//        this.localReceiverPort = localReceiverPort;
        this.udpReceiver = new DatagramSocket(localReceiverPort);
        for(int i=0;i<30;i++){//给server发送30个包，告诉server客户端的udpreceiver端口号
            tellNatPort();
        }
        window = new JFrame();
    }

    @Override
    public void run() {
        //audio准备
        try {
            sourceDataLine = AudioSystem.getSourceDataLine(format);
            sourceDataLine.open(format);
            sourceDataLine.start();
            System.out.println("audio播放开始");
        } catch (LineUnavailableException e) {
            e.printStackTrace();
        }


        //video准备
        this.window.setTitle("Receiver: " + localReceiverPort);
        this.window.setSize(new Dimension(640, 480));
        this.window.setVisible(true);
        this.window.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        this.g = this.window.getGraphics();


        while (!Thread.interrupted()) {
            byte[] bytes = new byte[1024 * 60];
            datagramPacket = new DatagramPacket(bytes, 0, bytes.length);
            try {
                udpReceiver.receive(datagramPacket);
                System.out.println("11111111111111");
            } catch (IOException e) {
                e.printStackTrace();
            }
            byte[] data = datagramPacket.getData();

            //从data中先读出realdata的长度，再独户realdata
            ByteBuffer byteBuffer = ByteBuffer.wrap(data);

            //读出第一个int，分辨这是一个video数据包还是audio数据包。1代表video，2代表audio
            int realDataLength = byteBuffer.getInt();

//            System.out.println("udp packet: len:"+realDataLength);

            int VAFlag = byteBuffer.getInt();

            if (VAFlag == 1) {//Video数据包
                System.out.println("a video data");
//                System.out.println("receive: realDataLength: " + realDataLength);//TODO---调试用
                try {
                    trans(Arrays.copyOfRange(data, 8, realDataLength + 8));
                } catch (InterruptedException e) {
                    break;
                } catch (IOException e) {
                    e.printStackTrace();
                }
            } else if (VAFlag == 2) {//Audio数据包
                System.out.println("a audio data");
                sourceDataLine.write(data, 8, realDataLength+8);//播放声音
            }
        }

        System.out.println("udp receiver 正在关闭camera 和 webcam");
        sourceDataLine.close();
        window.dispose();
    }


    public void trans(byte[] data) throws IOException, InterruptedException {
        ByteArrayInputStream stream = new ByteArrayInputStream(data);
        BufferedImage image = ImageIO.read(stream);
        g.drawImage(image, 0, 0, null);//将读到的图片进行绘制
//            System.out.println(read == bytes.length);
//        System.out.println("data.length: " + data.length);//TODO----调试用
    }

    private void tellNatPort() throws IOException {
        //将头信息
        ByteBuffer byteBuffer = ByteBuffer.allocate(2*Integer.BYTES);
        byteBuffer.putInt(0,ID);//此用户的ID
        byteBuffer.putInt(4,3);//代表这是一个告知port用的数据包
        byte[] info = byteBuffer.array();
        //TODO--调试用
//        System.out.println("send:-----data.len: "+data.length+"-----len.len: "+info.length+"-----newData.len:"+newData.length);
        datagramPacket = new DatagramPacket(info, 0, info.length, remoteReceiver);
        udpReceiver.send(datagramPacket);
    }
}
