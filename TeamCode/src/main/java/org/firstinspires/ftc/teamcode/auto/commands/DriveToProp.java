package org.firstinspires.ftc.teamcode.auto.commands;

import com.acmerobotics.roadrunner.geometry.Pose2d;
import com.acmerobotics.roadrunner.geometry.Vector2d;
import com.arcrobotics.ftclib.command.CommandBase;

import org.firstinspires.ftc.teamcode.bot.subsystems.CenterstageVision;
import org.firstinspires.ftc.teamcode.bot.subsystems.MecanumDriveBase;
import org.firstinspires.ftc.teamcode.drive.FieldConstants;
import org.firstinspires.ftc.teamcode.drive.Mecanum;
import org.firstinspires.ftc.teamcode.opencv.pipeline.CenterStagePipeline;

public class DriveToProp extends CommandBase {

    private final Mecanum m_drive;
    private final CenterstageVision m_cv;

    private final FieldConstants.Side m_side;
    private final FieldConstants.Stage m_stage;

    public DriveToProp(
            MecanumDriveBase driveBase,
            CenterstageVision cv,
            FieldConstants.Side side,
            FieldConstants.Stage stage
    ){
        m_drive = driveBase.getDrive();
        m_cv = cv;

        m_side = side;
        m_stage = stage;

        addRequirements(driveBase, cv);
    }

    @Override
    public void initialize() {
        CenterStagePipeline.PropPosition propPos = m_cv.getPropPosition();

        boolean redSide = m_side == FieldConstants.Side.RED;
        double yScale = redSide ? 1 : -1;

        //For BLUE side swap left and right prop positions
        if(!redSide){
            if(propPos == CenterStagePipeline.PropPosition.LEFT) propPos = CenterStagePipeline.PropPosition.RIGHT;
            else if(propPos == CenterStagePipeline.PropPosition.RIGHT) propPos = CenterStagePipeline.PropPosition.LEFT;
        }

        if(m_stage == FieldConstants.Stage.BACK) {
            switch (propPos) {
                case LEFT:
                    m_drive.followTrajectory(m_drive.trajectoryBuilder(m_drive.getPoseEstimate())
                            .splineTo(new Vector2d(9, -35 * yScale), Math.toRadians(180))
                            .build());
                    break;
                case CENTER:
                    m_drive.followTrajectory(m_drive.trajectoryBuilder(m_drive.getPoseEstimate())
                            .splineTo(new Vector2d(16, -34 * yScale), Math.toRadians(90 * yScale))
                            .build());
                    break;
                case RIGHT:
                    m_drive.followTrajectory(m_drive.trajectoryBuilder(m_drive.getPoseEstimate())
                            .lineToLinearHeading(new Pose2d(33, -35 * yScale, Math.toRadians(180)))
                            .build());
                    break;
            }
        } else {
            switch (propPos) {
                case LEFT:
                    m_drive.followTrajectory(m_drive.trajectoryBuilder(m_drive.getPoseEstimate())
                            .forward(20)
                            .splineTo(new Vector2d(-45, -20 * yScale), yScale * Math.toRadians(270))
                            .build());
                    break;
                case CENTER:
                    m_drive.followTrajectory(m_drive.trajectoryBuilder(m_drive.getPoseEstimate())
                            .lineToLinearHeading(new Pose2d(-42, -24 * yScale, 0))
                            .build());
                    break;
                case RIGHT:
                    m_drive.followTrajectory(m_drive.trajectoryBuilder(m_drive.getPoseEstimate())
                            .splineTo(new Vector2d(-33, -35 * yScale), 0)
                            .build());
                    break;
            }
        }
    }

    @Override
    public boolean isFinished() {
        return !m_drive.isBusy();
    }
}
