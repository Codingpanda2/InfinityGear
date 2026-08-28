package com.infinitygear.station;

import com.infinitypickaxes.InfinityPickaxes;
import org.bukkit.block.Block;
import org.bukkit.event.block.BlockPistonExtendEvent;
import org.bukkit.event.block.BlockPistonRetractEvent;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.mockito.Mockito.*;

class StationListenerTest {
    @Test void pistonMovementInvalidatesEveryMovedBinding() {
        StationManager manager = mock(StationManager.class);
        Block first = mock(Block.class), second = mock(Block.class);
        BlockPistonExtendEvent extend = mock(BlockPistonExtendEvent.class);
        BlockPistonRetractEvent retract = mock(BlockPistonRetractEvent.class);
        when(extend.getBlocks()).thenReturn(List.of(first, second));
        when(retract.getBlocks()).thenReturn(List.of(first));
        StationListener listener = new StationListener(mock(InfinityPickaxes.class), manager);

        listener.onPistonExtend(extend);
        listener.onPistonRetract(retract);

        verify(manager, times(2)).unbind(first);
        verify(manager).unbind(second);
    }
}
