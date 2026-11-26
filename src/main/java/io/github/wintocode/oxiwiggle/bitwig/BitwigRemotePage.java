package io.github.wintocode.oxiwiggle.bitwig;

import com.bitwig.extension.controller.api.ControllerHost;
import com.bitwig.extension.controller.api.CursorRemoteControlsPage;
import com.bitwig.extension.controller.api.CursorTrack;
import com.bitwig.extension.controller.api.DeviceBank;
import com.bitwig.extension.controller.api.PinnableCursorDevice;
import com.bitwig.extension.controller.api.RemoteControl;
import com.bitwig.extension.controller.api.SettableRangedValue;
import com.bitwig.extension.controller.api.TrackBank;
import com.bitwig.extension.controller.api.TrackBankContentFilter;
import io.github.wintocode.oxiwiggle.display.PageText;
import io.github.wintocode.oxiwiggle.surface.RemotePage;
import java.util.function.BiConsumer;

public final class BitwigRemotePage implements RemotePage {
    private final CursorTrack track;
    private final PinnableCursorDevice device;
    private final CursorRemoteControlsPage page;
    private final DeviceBank siblings;
    private final TrackBank visibleTracks;
    private final RemoteControl[] slots = new RemoteControl[SLOT_COUNT];
    private final SettableRangedValue[] values = new SettableRangedValue[SLOT_COUNT];

    public BitwigRemotePage(ControllerHost host, CursorTrack track) {
        this.track = track;
        visibleTracks = host.createTrackBank(1, 0, 0, true);
        visibleTracks.setContentFilter(TrackBankContentFilter.ALL_VISIBLE_CHANNELS);
        visibleTracks.followCursorTrack(track);
        device = track.createCursorDevice();
        siblings = device.createSiblingsDeviceBank(1);
        page = device.createCursorRemoteControlsPage(SLOT_COUNT);
        for (int i = 0; i < SLOT_COUNT; i++) {
            slots[i] = page.getParameter(i);
            values[i] = slots[i].value();
        }
    }

    public void observe(Runnable changed, Runnable selectionChanged,
                        BiConsumer<PagePosition, String> trackPositionChanged) {
        track.name().addValueObserver(ignored -> selectionChanged.run());
        track.position().addValueObserver(ignored -> selectionChanged.run());
        track.hasPrevious().markInterested();
        track.hasNext().markInterested();
        track.isGroup().markInterested();
        visibleTracks.scrollPosition().markInterested();
        visibleTracks.itemCount().markInterested();
        Runnable updateTrackPosition = () -> {
            trackPositionChanged.accept(trackPosition(), trackId());
            changed.run();
        };
        track.exists().addValueObserver(ignored -> {
            selectionChanged.run();
            updateTrackPosition.run();
        });
        track.channelId().addValueObserver(ignored -> {
            selectionChanged.run();
            updateTrackPosition.run();
        });
        visibleTracks.scrollPosition().addValueObserver(ignored -> updateTrackPosition.run());
        visibleTracks.itemCount().addValueObserver(ignored -> updateTrackPosition.run());
        device.exists().addValueObserver(ignored -> selectionChanged.run());
        device.name().addValueObserver(ignored -> selectionChanged.run());
        device.position().addValueObserver(ignored -> selectionChanged.run());
        siblings.itemCount().addValueObserver(ignored -> changed.run());
        device.hasPrevious().markInterested();
        device.hasNext().markInterested();
        page.pageCount().addValueObserver(ignored -> changed.run());
        page.selectedPageIndex().addValueObserver(ignored -> selectionChanged.run());
        page.getName().addValueObserver(ignored -> selectionChanged.run());
        for (int i = 0; i < SLOT_COUNT; i++) {
            slots[i].exists().addValueObserver(ignored -> changed.run());
            slots[i].name().addValueObserver(ignored -> changed.run());
            values[i].addValueObserver(ignored -> changed.run());
            values[i].displayedValue().addValueObserver(ignored -> changed.run());
            values[i].discreteValueCount().addValueObserver(ignored -> changed.run());
        }
    }

    private boolean hasPage(int pageIndex) {
        return track.exists().get() && device.exists().get()
                && pageIndex >= 0 && pageIndex < page.pageCount().get();
    }

    @Override public String title() {
        int pageIndex = page.selectedPageIndex().get();
        return hasPage(pageIndex) ? PageText.title(page.getName().get(), pageIndex) : "OxiWiggle";
    }

    @Override public PagePosition pagePosition() {
        if (!track.exists().get() || !device.exists().get()) return new PagePosition(-1, 0);
        return new PagePosition(page.selectedPageIndex().get(), page.pageCount().get());
    }

    @Override public void navigatePage(int direction) {
        if (!hasPage(page.selectedPageIndex().get())) return;
        if (direction > 0) page.selectNextPage(false);
        else page.selectPreviousPage(false);
    }

    @Override public String deviceName() {
        if (!track.exists().get() || !device.exists().get()) return "No device";
        return PageText.field(device.name().get(), Integer.MAX_VALUE, "Device");
    }

    @Override public PagePosition devicePosition() {
        if (!track.exists().get() || !device.exists().get()) return new PagePosition(-1, 0);
        return new PagePosition(device.position().get(), siblings.itemCount().get());
    }

    @Override public void navigateDevice(int direction) {
        if (!track.exists().get() || !device.exists().get()) return;
        if (direction > 0 && device.hasNext().get()) device.selectNext();
        if (direction < 0 && device.hasPrevious().get()) device.selectPrevious();
    }

    @Override public String trackName() {
        if (!track.exists().get()) return "No track";
        return PageText.field(track.name().get(), Integer.MAX_VALUE, "Track");
    }

    @Override public String trackId() {
        return track.exists().get() ? track.channelId().get() : "";
    }

    @Override public PagePosition trackPosition() {
        if (!track.exists().get()) return new PagePosition(-1, 0);
        return new PagePosition(visibleTracks.scrollPosition().get(), visibleTracks.itemCount().get());
    }

    @Override public boolean navigateTrack(int direction) {
        if (!track.exists().get()) return false;
        if (direction > 0 && track.hasNext().get()) {
            track.selectNext();
            return true;
        }
        if (direction < 0 && track.hasPrevious().get()) {
            track.selectPrevious();
            return true;
        }
        return false;
    }

    @Override public void toggleTrackGroup() {
        if (track.exists().get() && track.isGroup().get()) track.isGroupExpanded().toggle();
    }

    @Override public Reading read(int index) {
        if (!hasPage(page.selectedPageIndex().get()) || !slots[index].exists().get()) {
            return new Reading("", false, 0, 0);
        }
        var value = values[index];
        return new Reading(slots[index].name().get(), true, value.get(),
                value.discreteValueCount().get(), value.displayedValue().get());
    }

    @Override public void increment(int index, double normalizedDelta) {
        values[index].inc(normalizedDelta);
    }
}
