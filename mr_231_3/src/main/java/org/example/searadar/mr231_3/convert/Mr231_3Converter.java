package org.example.searadar.mr231_3.convert;

import org.apache.camel.Exchange;
import ru.oogis.searadar.api.convert.SearadarExchangeConverter;
import ru.oogis.searadar.api.message.*;
import ru.oogis.searadar.api.types.IFF;
import ru.oogis.searadar.api.types.TargetStatus;
import ru.oogis.searadar.api.types.TargetType;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;

public class Mr231_3Converter implements SearadarExchangeConverter {

    private static final Double[] DISTANCE_SCALE = {0.125, 0.25, 0.5, 1.5, 3.0, 6.0, 12.0, 24.0, 48.0, 96.0};

    private String[] fields;
    private String ctrlSum;
    private String msgType;

    @Override
    public List<SearadarStationMessage> convert(Exchange exchange) {
        return convert(exchange.getIn().getBody(String.class));
    }

    public List<SearadarStationMessage> convert(String message) {

        List<SearadarStationMessage> msgList = new ArrayList<>();

        readFields(message);

        switch (msgType) {

            case "TTM" : msgList.add(getTTM());
                break;

            case "RSD" : {

                RadarSystemDataMessage rsd = getRSD();
                InvalidMessage invalidMessage = checkRSD(rsd);

                if (invalidMessage != null)  msgList.add(invalidMessage);
                else msgList.add(rsd);
                break;
            }

        }

        return msgList;
    }


    private void readFields(String msg) {

        String temp = msg.substring( 3, msg.indexOf("*") ).trim();

        fields = temp.split(Pattern.quote(","));
		ctrlSum = msg.substring( msg.indexOf("*") + 1 ).trim();
       
        msgType = fields[0];

    }

	public class RadarSystemDataMessage3 extends RadarSystemDataMessage {

		private Integer controlSum;


		public Integer getControlSum() {
			return controlSum;
		}
	
		public void setControlSum(Integer controlSum) {
			this.controlSum = controlSum;
		}


		@Override
    	public String toString() {
			return "RadarSystemData{" +
					"msgRecTime=" + getMsgRecTime() +
					", initialDistance=" + getInitialDistance() +
					", initialBearing=" + getInitialBearing() +
					", movingCircleOfDistance=" + getMovingCircleOfDistance() +
					", bearing=" + getBearing() +
					", distanceFromShip=" + getDistanceFromShip() +
					", bearing2=" + getBearing2() +
					", distanceScale=" + getDistanceScale() +
					", distanceUnit=" + getDistanceUnit() +
					", displayOrientation=" + getDisplayOrientation() +
					", workingMode=" + getWorkingMode() +
					", controlSum=" + getControlSum() +
					'}';
    	}

	}


	public class TrackedTargetMessage3 extends TrackedTargetMessage {

		private Long period;


		public Long getPeriod() {
			return period;
		}
	
		public void setPeriod(Long period) {
			this.period = period;
		}

		@Override
    	public String toString() {
			return "TrackedTargetMessage{" +
					"msgRecTime=" + getMsgRecTime() +
					", msgTime=" + getMsgTime() +
					", targetNumber=" + getTargetNumber() +
					", distance=" + getDistance() +
					", bearing=" + getBearing() +
					", course=" + getCourse() +
					", speed=" + getSpeed() +
					", type=" + getType() +
					", status=" + getStatus() +
					", iff=" + getIff() +
					", period=" + getPeriod() +
					'}';
    	}
	}

    private TrackedTargetMessage getTTM() {
        
        TrackedTargetMessage3 ttm3 = new TrackedTargetMessage3();

        Long msgRecTimeMillis = System.currentTimeMillis();

        ttm3.setMsgTime(msgRecTimeMillis);
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

        ttm3.setMsgRecTime(new Timestamp(System.currentTimeMillis()));
        ttm3.setTargetNumber(Integer.parseInt(fields[1]));
        ttm3.setDistance(Double.parseDouble(fields[2]));
        ttm3.setBearing(Double.parseDouble(fields[3]));
        ttm3.setCourse(Double.parseDouble(fields[6]));
        ttm3.setSpeed(Double.parseDouble(fields[5]));
        ttm3.setStatus(status);
        ttm3.setIff(iff);
		ttm3.setPeriod(Long.parseLong(fields[14]));

        ttm3.setType(type);

        return ttm3;
    }

    private RadarSystemDataMessage3 getRSD() {

        RadarSystemDataMessage3 rsd3 = new RadarSystemDataMessage3();

        rsd3.setMsgRecTime(new Timestamp(System.currentTimeMillis()));
        rsd3.setInitialDistance(Double.parseDouble(fields[1]));
        rsd3.setInitialBearing(Double.parseDouble(fields[2]));
        rsd3.setMovingCircleOfDistance(Double.parseDouble(fields[3]));
        rsd3.setBearing(Double.parseDouble(fields[4]));
        rsd3.setDistanceFromShip(Double.parseDouble(fields[9]));
        rsd3.setBearing2(Double.parseDouble(fields[10]));
        rsd3.setDistanceScale(Double.parseDouble(fields[11]));
        rsd3.setDistanceUnit(fields[12]);
        rsd3.setDisplayOrientation(fields[13]);
        rsd3.setWorkingMode(fields[14]);
		rsd3.setControlSum(Integer.parseInt(ctrlSum));

        return rsd3;
    }

    private InvalidMessage checkRSD(RadarSystemDataMessage rsd) {

        InvalidMessage invalidMessage = new InvalidMessage();
        String infoMsg = "";

        if (!Arrays.asList(DISTANCE_SCALE).contains(rsd.getDistanceScale())) {

            infoMsg = "RSD message. Wrong distance scale value: " + rsd.getDistanceScale();
            invalidMessage.setInfoMsg(infoMsg);
            return invalidMessage;
        }

        return null;
    }

}
