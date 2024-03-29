package org.firstinspires.ftc.teamcode.bot.commands;

import com.arcrobotics.ftclib.command.InstantCommand;
import com.arcrobotics.ftclib.command.ParallelCommandGroup;
import com.arcrobotics.ftclib.command.ParallelRaceGroup;
import com.arcrobotics.ftclib.command.SequentialCommandGroup;
import com.arcrobotics.ftclib.command.WaitCommand;

import org.firstinspires.ftc.teamcode.bot.subsystems.ClawArm;
import org.firstinspires.ftc.teamcode.bot.subsystems.DualLinearSlide;

public class RetractLinearSlide extends SequentialCommandGroup {

    public RetractLinearSlide(DualLinearSlide slide, ClawArm clawArm){
        addCommands(
                new MoveArm(clawArm, ClawArm.ArmState.RAISED),
                new WaitCommand(400),
                new MoveSlideToPosition(slide, 10)
        );
        addRequirements(slide);
    }
}
