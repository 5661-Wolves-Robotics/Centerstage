package org.firstinspires.ftc.teamcode.bot;

import androidx.annotation.NonNull;

import com.acmerobotics.dashboard.FtcDashboard;
import com.acmerobotics.dashboard.config.Config;
import com.acmerobotics.dashboard.telemetry.MultipleTelemetry;
import com.acmerobotics.dashboard.telemetry.TelemetryPacket;
import com.acmerobotics.roadrunner.geometry.Pose2d;
import com.acmerobotics.roadrunner.geometry.Vector2d;
import com.qualcomm.hardware.modernrobotics.ModernRoboticsI2cRangeSensor;
import com.qualcomm.hardware.rev.Rev2mDistanceSensor;
import com.qualcomm.robotcore.hardware.DcMotor;
import com.qualcomm.robotcore.hardware.DcMotorEx;
import com.qualcomm.robotcore.hardware.DcMotorSimple;
import com.qualcomm.robotcore.hardware.HardwareMap;
import com.qualcomm.robotcore.hardware.Servo;
import com.qualcomm.robotcore.util.ElapsedTime;
import com.qualcomm.robotcore.util.Range;

import org.checkerframework.checker.units.qual.C;
import org.firstinspires.ftc.robotcore.external.Telemetry;
import org.firstinspires.ftc.robotcore.external.hardware.camera.CameraName;
import org.firstinspires.ftc.robotcore.external.navigation.DistanceUnit;
import org.firstinspires.ftc.teamcode.drive.Mecanum;
import org.firstinspires.ftc.teamcode.teleop.TeleOpMain;
import org.firstinspires.ftc.teamcode.util.CameraStream;
import org.firstinspires.ftc.teamcode.util.motor.LimitedMotor;
import org.firstinspires.ftc.teamcode.util.servo.ToggleServo;
import org.firstinspires.ftc.vision.VisionPortal;
import org.firstinspires.ftc.vision.apriltag.AprilTagDetection;
import org.firstinspires.ftc.vision.apriltag.AprilTagGameDatabase;
import org.firstinspires.ftc.vision.apriltag.AprilTagProcessor;

import java.util.List;

@Config
public class CenterStageBot extends Robot<Mecanum>{

    //CAMERA
    private CameraName cam = null;

    private VisionPortal vision;
    private AprilTagProcessor aprilTagProcessor;
    private CameraStream cameraStream;

    //INTAKE
    public DcMotor intake = null;
    public ToggleServo dropdown = null;
    private static final float LOWERED_DROPDOWN = 0.23f;
    private static final float RAISED_DROPDOWN = 0.66f;
    private boolean dropped = false;

    //CLAW
    public ToggleServo claw = null;
    private static final float OPEN_CLAW = 0.21f;
    private static final float CLOSED_CLAW = 0.0f;

    public Servo rightArm = null;
    public Servo leftArm = null;
    public static final float RAISED_ARM = 0.30f;
    public static final float STORED_ARM = 0.22f;
    public static final float LOWERED_ARM = 0.16f;

    public enum ArmState {
        LOWERED,
        RAISED,
        STORED
    }

    private ArmState armState = ArmState.STORED;

    //SLIDE
    public DcMotorEx leftSlide = null;
    public DcMotorEx rightSlide = null;
    private static final int MAX_SLIDE_EXTENSION = 4300;
    private static final float SLIDE_STATIC = 0.1f;
    private double slidePos = 0;
    private final double SLIDE_TPR = 537.7;
    private final double SLIDE_RPM = 312;

    public enum SlideState {
        IDLE,
        EXTENDING,
        RETRACTING,
        STORING
    }
    private SlideState slideState = SlideState.IDLE;
    private double retractTime = 0;
    private double slideDelay = 0;

    //LAUNCHER
    public ToggleServo launcher = null;
    private static final float LAUNCHER_DOWN = 0.0f;
    private static final float LAUNCHER_UP = 0.5f;

    //RANGE
    Rev2mDistanceSensor rangeSensor = null;
    private static final Vector2d RANGE_POS = new Vector2d(-8.6, 0);

    private double prevTime = 0;
    private double deltaTime = 0;

    public CenterStageBot() {super();}

    @Override
    public void init(HardwareMap hardwareMap, Pose2d pose, MultipleTelemetry telemetry){
        super.init(hardwareMap, new Mecanum(hardwareMap), pose, telemetry);

        drive.setMode(DcMotor.RunMode.RUN_WITHOUT_ENCODER);

        leftSlide = hardwareMap.get(DcMotorEx.class, "leftSlide");
        leftSlide.setZeroPowerBehavior(DcMotor.ZeroPowerBehavior.BRAKE);
        leftSlide.setDirection(DcMotorSimple.Direction.REVERSE);
        leftSlide.setMode(DcMotor.RunMode.STOP_AND_RESET_ENCODER);

        rightSlide = hardwareMap.get(DcMotorEx.class, "rightSlide");
        rightSlide.setZeroPowerBehavior(DcMotor.ZeroPowerBehavior.BRAKE);
        rightSlide.setMode(DcMotor.RunMode.STOP_AND_RESET_ENCODER);

        leftSlide.setTargetPosition((int)slidePos);
        rightSlide.setTargetPosition((int)slidePos);

        leftSlide.setPower(1.0);
        rightSlide.setPower(1.0);

        rightSlide.setMode(DcMotor.RunMode.RUN_TO_POSITION);
        leftSlide.setMode(DcMotor.RunMode.RUN_TO_POSITION);

        intake = hardwareMap.get(DcMotor.class, "perpendicularEncoder");

        dropdown = new ToggleServo(hardwareMap.get(Servo.class, "dropdown"), RAISED_DROPDOWN, LOWERED_DROPDOWN, true, true);

        claw = new ToggleServo(hardwareMap.get(Servo.class, "claw"), OPEN_CLAW, CLOSED_CLAW, true, true);

        Servo servo = hardwareMap.get(Servo.class, "launcher");
        launcher = new ToggleServo(servo, LAUNCHER_DOWN, LAUNCHER_UP, false, true);

        leftArm = hardwareMap.get(Servo.class, "arm1");
        rightArm = hardwareMap.get(Servo.class, "arm2");

        rangeSensor = hardwareMap.get(Rev2mDistanceSensor.class, "range");

        setArmPos(STORED_ARM);

        cam = hardwareMap.get(CameraName.class, "Camera");

        initAprilTag();

        time.reset();
    }

    private void initAprilTag(){
        aprilTagProcessor = new AprilTagProcessor.Builder()
                .setTagFamily(AprilTagProcessor.TagFamily.TAG_36h11)
                .setTagLibrary(AprilTagGameDatabase.getCenterStageTagLibrary())
                .build();

        cameraStream = new CameraStream();

        vision = new VisionPortal.Builder()
                .setCamera(cam)
                .addProcessor(cameraStream)
                .addProcessor(aprilTagProcessor)
                .build();

        FtcDashboard.getInstance().startCameraStream(cameraStream, 0);
    }

    public void setSlidePower(double pow){
        slidePos = Range.clip(slidePos + ((SLIDE_RPM / 60) * SLIDE_TPR) * deltaTime * pow, 0, MAX_SLIDE_EXTENSION);
        leftSlide.setTargetPosition((int)slidePos);
        rightSlide.setTargetPosition((int)slidePos);
    }

    public void setSlidePos(int pos){
        slidePos = pos;
        leftSlide.setTargetPosition((int)slidePos);
        rightSlide.setTargetPosition((int)slidePos);
    }

    public void setArmPos(double pos){
        leftArm.setPosition(pos);
        rightArm.setPosition(pos);
    }

    public void retractSlide(double delay){
        retractTime = time.milliseconds();
        slideState = SlideState.RETRACTING;
        slideDelay = delay;
    }

    public double getSlidePos(){
        return (leftSlide.getCurrentPosition() + rightSlide.getCurrentPosition()) / 2.0;
    }

    public double getDist(){
        return rangeSensor.getDistance(DistanceUnit.INCH);
    }

    public void updateSlide(){
        double slidePos = getSlidePos();
        switch(slideState){
            case EXTENDING:{
                if(slidePos > 0.23) setArmState(ArmState.LOWERED);
                slideState = SlideState.IDLE;
                break;
            }
            case RETRACTING:{
                if(time.milliseconds() - retractTime >= slideDelay){
                    setArmState(ArmState.RAISED);
                }
                if(time.milliseconds() - retractTime >= slideDelay + 400){
                    setSlidePos(0);

                    slideState = SlideState.STORING;
                }
                break;
            }
            case STORING:{
                if(slidePos < 920){
                    setArmState(ArmState.STORED);
                }
                break;
            }
            default:{
                break;
            }
        }
    }

    public boolean isSlideBusy(){
        return leftSlide.isBusy() || rightSlide.isBusy();
    }

    public boolean isIntakeDropped(){
        return dropped;
    }

    public void toggleIntake(){
        dropped = !dropped;
        dropdown.toggle();
        if(dropped) intake.setPower(0.7);
        else intake.setPower(0.0);
    }

    public void launchPlane(){
        launcher.toggle();
    }

    public SlideState getSlideState(){
        return slideState;
    }

    public void setArmState(ArmState state){
        armState = state;

        switch(state){
            case LOWERED:
                setArmPos(LOWERED_ARM);
                break;
            case RAISED:
                setArmPos(RAISED_ARM);
                break;
            case STORED:
                setArmPos(STORED_ARM);
                break;
        }
    }

    public ArmState getArmState(){
        return armState;
    }

    public List<AprilTagDetection> getAprilTags(){
        return aprilTagProcessor.getDetections();
    }

    @Override
    public void update() {
        TelemetryPacket packet = new TelemetryPacket();

        Pose2d poseEstimate = drive.getPoseEstimate();
        Vector2d distPoint = RANGE_POS.plus(new Vector2d(-getDist(), 0)).rotated(poseEstimate.getHeading()).plus(poseEstimate.vec());

        packet.fieldOverlay().fillCircle(distPoint.getX(), distPoint.getY(), 2);

        telemetry.addData("Slide", slidePos);
        updateSlide();

        telemetry.addData("Dist", getDist());
        drive.update();
        FtcDashboard.getInstance().sendTelemetryPacket(packet);
        telemetry.update();

        double newTime = time.seconds();
        deltaTime = newTime - prevTime;
        prevTime = newTime;
    }
}