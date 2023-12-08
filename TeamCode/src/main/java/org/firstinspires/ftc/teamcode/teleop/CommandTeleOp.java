package org.firstinspires.ftc.teamcode.teleop;

import static org.firstinspires.ftc.teamcode.bot.subsystems.ClawArm.ArmState.LOWERED;
import static org.firstinspires.ftc.teamcode.bot.subsystems.ClawArm.ArmState.RAISED;

import com.acmerobotics.dashboard.FtcDashboard;
import com.arcrobotics.ftclib.command.CommandOpMode;
import com.arcrobotics.ftclib.command.CommandScheduler;
import com.arcrobotics.ftclib.command.ConditionalCommand;
import com.arcrobotics.ftclib.command.InstantCommand;
import com.arcrobotics.ftclib.command.ParallelCommandGroup;
import com.arcrobotics.ftclib.command.RunCommand;
import com.arcrobotics.ftclib.command.SequentialCommandGroup;
import com.arcrobotics.ftclib.gamepad.GamepadEx;
import com.arcrobotics.ftclib.gamepad.GamepadKeys;
import com.qualcomm.robotcore.eventloop.opmode.TeleOp;

import org.firstinspires.ftc.robotcore.external.hardware.camera.WebcamName;
import org.firstinspires.ftc.teamcode.bot.commands.AutoLowerArm;
import org.firstinspires.ftc.teamcode.bot.commands.Delay;
import org.firstinspires.ftc.teamcode.bot.commands.LocalDrive;
import org.firstinspires.ftc.teamcode.bot.commands.MoveArm;
import org.firstinspires.ftc.teamcode.bot.commands.PlacerDrive;
import org.firstinspires.ftc.teamcode.bot.commands.PowerSlide;
import org.firstinspires.ftc.teamcode.bot.commands.RetractLinearSlide;
import org.firstinspires.ftc.teamcode.bot.commands.ToggleIntake;
import org.firstinspires.ftc.teamcode.bot.subsystems.CenterstageVision;
import org.firstinspires.ftc.teamcode.bot.subsystems.ClawArm;
import org.firstinspires.ftc.teamcode.bot.subsystems.DroneLauncher;
import org.firstinspires.ftc.teamcode.bot.subsystems.DualLinearSlide;
import org.firstinspires.ftc.teamcode.bot.subsystems.Intake;
import org.firstinspires.ftc.teamcode.bot.subsystems.MecanumDriveBase;
import org.firstinspires.ftc.teamcode.drive.Mecanum;
import org.firstinspires.ftc.teamcode.opencv.pipeline.CenterStagePipeline;
import org.firstinspires.ftc.teamcode.util.CameraStream;
import org.firstinspires.ftc.teamcode.util.PS4GamepadEx;
import org.firstinspires.ftc.vision.VisionPortal;
import org.firstinspires.ftc.vision.apriltag.AprilTagGameDatabase;
import org.firstinspires.ftc.vision.apriltag.AprilTagProcessor;

@TeleOp
public class CommandTeleOp extends CommandOpMode {

    GamepadEx driver, placer;

    MecanumDriveBase drive;
    LocalDrive localDrive;
    PlacerDrive placerDrive;
    DualLinearSlide slide;
    PowerSlide powerSlide;
    SequentialCommandGroup retractSlide;
    Intake intake;
    ToggleIntake toggleIntake;
    ClawArm clawArm;
    AutoLowerArm autoLowerArm;
    MoveArm raiseArm;
    MoveArm lowerArm;
    DroneLauncher droneLauncher;

    CenterstageVision cv;



    @Override
    public void initialize() {
        driver = new GamepadEx(gamepad1);
        placer = new GamepadEx(gamepad2);

        drive = new MecanumDriveBase(hardwareMap);
        clawArm = new ClawArm(hardwareMap, "arm1", "claw");
        slide = new DualLinearSlide(hardwareMap, "rightSlide", "leftSlide", 4300);
        intake = new Intake(hardwareMap, "dropdown", "perpendicularEncoder");
        droneLauncher = new DroneLauncher(hardwareMap, "launcher");
        cv = new CenterstageVision(hardwareMap, "Camera");

        localDrive = new LocalDrive(drive, driver::getLeftX, driver::getLeftY, driver::getRightX);
        placerDrive = new PlacerDrive(drive, placer::getLeftX, placer::getLeftY, Math.toRadians(90));

        powerSlide = new PowerSlide(slide, ()-> driver.getTrigger(GamepadKeys.Trigger.RIGHT_TRIGGER) - driver.getTrigger(GamepadKeys.Trigger.LEFT_TRIGGER));
        retractSlide = new SequentialCommandGroup(
                new InstantCommand(clawArm::open, clawArm),
                new Delay(200),
                new RetractLinearSlide(slide, clawArm)
        );

        autoLowerArm = new AutoLowerArm(slide, clawArm);
        raiseArm = new MoveArm(clawArm, RAISED);
        lowerArm = new MoveArm(clawArm, LOWERED);

        toggleIntake = new ToggleIntake(intake);

        driver.getGamepadButton(GamepadKeys.Button.X)
                .whenPressed(toggleIntake);
        driver.getGamepadButton(GamepadKeys.Button.B)
                .whenPressed(new InstantCommand(intake::reverse, intake))
                .whenReleased(new ConditionalCommand(
                        new InstantCommand(intake::power, intake),
                        new InstantCommand(intake::disable, intake),
                        ()-> intake.getState() == Intake.IntakeState.LOWERED
                ));
        driver.getGamepadButton(GamepadKeys.Button.A)
                .whenPressed(new ParallelCommandGroup(
                        placerDrive,
                        new InstantCommand(clawArm::close, clawArm)
                ));

        placer.getGamepadButton(GamepadKeys.Button.DPAD_UP)
                .whenPressed(raiseArm);
        placer.getGamepadButton(GamepadKeys.Button.DPAD_DOWN)
                .whenPressed(lowerArm);
        placer.getGamepadButton(GamepadKeys.Button.Y)
                .whenPressed(new InstantCommand(droneLauncher::launch, droneLauncher));
        placer.getGamepadButton(GamepadKeys.Button.A)
                .cancelWhenPressed(placerDrive)
                .whenPressed(retractSlide);

        register(drive);

        drive.setDefaultCommand(localDrive);
        slide.setDefaultCommand(powerSlide);
        clawArm.setDefaultCommand(autoLowerArm);

        telemetry.addData("PropPosition", cv::getPropPosition);

        schedule(new RunCommand(telemetry::update));
    }
}
