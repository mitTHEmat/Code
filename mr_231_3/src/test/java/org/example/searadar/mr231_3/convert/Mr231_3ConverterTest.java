package org.example.searadar.mr231_3.convert;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTimeout;
import static org.junit.jupiter.api.Assertions.assertTimeoutPreemptively;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.sql.Timestamp;
import java.util.Collections;
import java.util.List;
import java.util.regex.Pattern;
import java.time.Duration;

import org.example.searadar.mr231_3.convert.Mr231_3Converter;
import org.example.searadar.mr231_3.convert.Mr231_3Converter.RadarSystemDataMessage3;
import org.example.searadar.mr231_3.convert.Mr231_3Converter.TrackedTargetMessage3;
import org.example.searadar.mr231_3.station.Mr231_3StationType;
import ru.oogis.searadar.api.message.SearadarStationMessage;
import ru.oogis.searadar.api.types.IFF;
import ru.oogis.searadar.api.types.TargetStatus;
import ru.oogis.searadar.api.types.TargetType;
import scala.jdk.DurationConverters;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;


public class Mr231_3ConverterTest {
    
    private String[] fields;
    private String ctrlSum; // Поле для хранения контрольной суммы
    private String msgType;
    Mr231_3Converter converter = new Mr231_3Converter();
    
    private void readFields(String msg) {
        String temp = msg.substring( 3, msg.indexOf("*") ).trim();
        fields = temp.split(Pattern.quote(","));
		ctrlSum = msg.substring( msg.indexOf("*") + 1 ).trim(); // Вытаскиваем значение контрольной суммы из строки (все что после "*")
        msgType = fields[0];
    }


    @Test
    public void testConvertTTM() {
        // Контрольные примеры
        String ttmMessage = "$RATTM,66,28.71,341.1,T,57.6,024.5,T,0.4,4.1,N,b,L,,457362,А*42";
        String ttmMessage1 = "$RATTM,23,13.88,137.2,T,63.8,094.3,T,9.2,79.4,N,b,T,,783344,A*42"; // можно также использовать для проверки
        
        // парсинг параметров
        readFields(ttmMessage);

        TargetStatus status = TargetStatus.UNRELIABLE_DATA;
        IFF iff = IFF.UNKNOWN;
        TargetType type = TargetType.UNKNOWN;

        switch (fields[12]) {
            case "L" : status = TargetStatus.LOST;
                break;
            case "Q" : status = TargetStatus.UNRELIABLE_DATA;
                break;
            case "T" : status = TargetStatus.TRACKED;
                break;
        }

        switch (fields[11]) {
            case "b" : iff = IFF.FRIEND;
                break;
            case "p" : iff = IFF.FOE;
                break;
            case "d" : iff = IFF.UNKNOWN;
                break;
        }

        Mr231_3Converter.TrackedTargetMessage3 expectedTTM = converter.new TrackedTargetMessage3();

        // Задаем текущее время
        long currentTimeMillis = System.currentTimeMillis();
        Timestamp currentTimestamp = new Timestamp(currentTimeMillis);

        // Выполнение метода convert для TTM с ограничением по времени
        List<SearadarStationMessage> result = assertTimeoutPreemptively(
            Duration.ofSeconds(5),
            () -> converter.convert(ttmMessage)
        );

         // заполнение ожидаемого результата
        expectedTTM.setMsgRecTime(currentTimestamp);
        expectedTTM.setMsgTime(currentTimeMillis);
        expectedTTM.setTargetNumber(Integer.parseInt(fields[1]));
        expectedTTM.setDistance(Double.parseDouble(fields[2]));
        expectedTTM.setBearing(Double.parseDouble(fields[3]));
        expectedTTM.setCourse(Double.parseDouble(fields[6]));
        expectedTTM.setSpeed(Double.parseDouble(fields[5]));
        expectedTTM.setType(type);
        expectedTTM.setStatus(status);
        expectedTTM.setIff(iff);
        expectedTTM.setPeriod(Long.valueOf(fields[14]));

        // формируем ожидаемый результат в один объект
        List<SearadarStationMessage> waitRes = Collections.singletonList(expectedTTM);

        // Проверки
        assertEquals(waitRes.size(), result.size());
        assertTrue(result.get(0) instanceof Mr231_3Converter.TrackedTargetMessage3);

        Mr231_3Converter.TrackedTargetMessage3 actualTTM = (Mr231_3Converter.TrackedTargetMessage3) result.get(0);
        // Сравниваем ожидаемый и полученный результаты
        assertEquals(expectedTTM.getTargetNumber(), actualTTM.getTargetNumber());
        assertEquals(expectedTTM.getDistance(), actualTTM.getDistance(), 0.01);
        assertEquals(expectedTTM.getBearing(), actualTTM.getBearing(), 0.01);
        assertEquals(expectedTTM.getPeriod(), actualTTM.getPeriod());

        // Проверка времени с точностью до секунд и с учетом допустимой погрешности
        assertTrue(Math.abs(expectedTTM.getMsgRecTime().getTime() - actualTTM.getMsgRecTime().getTime()) <= 50);
        assertTrue(Math.abs(expectedTTM.getMsgTime() - actualTTM.getMsgTime()) <= 50);
    }

    @Test
    public void testConvertRSD() {
        // Контрольные примеры
        String rsdMessage = "$RARSD,36.5,331.4,8.4,320.6,,,,,11.6,185.3,96.0,N,N,S*33";
        String rsdMessage1 = "$RARSD,50.5,309.9,64.8,132.3,,,,,52.6,155.0,48.0,K,N,S*28"; // можно также использовать для проверки

        Mr231_3Converter.RadarSystemDataMessage3 expectedRSD = converter.new RadarSystemDataMessage3();
        
        // Выполнение метода convert для RSD с ограничением по времени
        List<SearadarStationMessage> result = assertTimeoutPreemptively(
            Duration.ofSeconds(5),
            () -> converter.convert(rsdMessage)
        );

        // парсинг параметров
        readFields(rsdMessage);

        // заполнение ожидаемого результата
        expectedRSD.setMsgRecTime(new Timestamp(System.currentTimeMillis()));
        expectedRSD.setInitialDistance(Double.parseDouble(fields[1]));
        expectedRSD.setInitialBearing(Double.parseDouble(fields[2]));
        expectedRSD.setMovingCircleOfDistance(Double.parseDouble(fields[3]));
        expectedRSD.setBearing(Double.parseDouble(fields[4]));
        expectedRSD.setDistanceFromShip(Double.parseDouble(fields[9]));
        expectedRSD.setBearing2(Double.parseDouble(fields[10]));
        expectedRSD.setDistanceScale(Double.parseDouble(fields[11]));
        expectedRSD.setDistanceUnit(fields[12]);
        expectedRSD.setDisplayOrientation(fields[13]);
        expectedRSD.setWorkingMode(fields[14]);
		expectedRSD.setControlSum(Integer.parseInt(ctrlSum));
        
        // формируем ожидаемый результат в один объект
        List<SearadarStationMessage> waitRes = Collections.singletonList(expectedRSD);

        // Проверки
        assertEquals(waitRes.size(), result.size());
        assertTrue(result.get(0) instanceof Mr231_3Converter.RadarSystemDataMessage3);

        Mr231_3Converter.RadarSystemDataMessage3 actualRSD = (Mr231_3Converter.RadarSystemDataMessage3) result.get(0);

        // Сравниваем ожидаемый и полученный результаты
        assertEquals(expectedRSD.getInitialDistance(), actualRSD.getInitialDistance());
        assertEquals(expectedRSD.getInitialBearing(), actualRSD.getInitialBearing());
        assertEquals(expectedRSD.getMovingCircleOfDistance(), actualRSD.getMovingCircleOfDistance());
        assertEquals(expectedRSD.getBearing(), actualRSD.getBearing(), 0.01);
        assertEquals(expectedRSD.getDistanceFromShip(), actualRSD.getDistanceFromShip());
        assertEquals(expectedRSD.getBearing2(), actualRSD.getBearing2());
        assertEquals(expectedRSD.getDistanceScale(), actualRSD.getDistanceScale());
        assertEquals(expectedRSD.getDistanceUnit(), actualRSD.getDistanceUnit());
        assertEquals(expectedRSD.getDisplayOrientation(), actualRSD.getDisplayOrientation());
        assertEquals(expectedRSD.getWorkingMode(), actualRSD.getWorkingMode());
        assertEquals(expectedRSD.getControlSum(), actualRSD.getControlSum());

        // Проверка времени с точностью до секунд и с учетом допустимой погрешности
        assertTrue(Math.abs(expectedRSD.getMsgRecTime().getTime() - actualRSD.getMsgRecTime().getTime()) <= 50);
    }
}
