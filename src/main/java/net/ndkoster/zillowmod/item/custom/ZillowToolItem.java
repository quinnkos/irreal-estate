package net.ndkoster.zillowmod.item.custom;

import java.util.*;

import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.properties.*;
import org.jetbrains.annotations.NotNull;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.VoxelShape;

public class ZillowToolItem extends Item {

    public ZillowToolItem(Properties pProperties) {
        super(pProperties);
    }

    static UseOnContext world;
    static Level level;
    static Player player;
    static InteractionHand hand;

    static int clicks = 0;

    static final Direction[] DIRECTIONS = {Direction.NORTH, Direction.EAST, Direction.SOUTH, Direction.WEST};
    static final double PLAYER_HEIGHT = 1.5;
    static final double PLAYER_WIDTH = 0.6;
    static final double MAX_JUMP_HEIGHT = 1.25;
    static final double MIN_VERTICAL_LIVING_SPACE = 0.5;
    static final double SQFT_MULTIPLIER = 10.7639;
    static final int MIN_BLOCKS_IN_ROOM = 6;

    static List<BlockPos> lotBounds = new ArrayList<>();
    static int minXOfLot;
    static int maxXOfLot;
    static int minZOfLot;
    static int maxZOfLot;

    static Set<BlockPos> closedList = new HashSet<>();
    static BlockPos origin;
    static Direction houseFacing;
    static List<BlockPos> perimeter;
    static int sqftOfLot;
    static int blocksInHouse = 0;
    static int sqftOfHouse = 0;
    static int rooms = 0;
    static int bedrooms = 0;
    static boolean isFurnished = false;

    @Override
    public @NotNull InteractionResult useOn(@NotNull UseOnContext pContext) {

        world = pContext;
        level = world.getLevel();
        player = world.getPlayer();
        hand = world.getHand();
        assert player != null;

        BlockPos clickedBlock = world.getClickedPos();

        //main loop
        if (level.isClientSide()) {

            //get first bound
            if (clicks == 0) {
                clicks++;
                lotBounds.add(clickedBlock);
                player.sendSystemMessage(Component.literal("First bound: (" + clickedBlock.getX() + ", " + clickedBlock.getZ() + ")"));
                player.sendSystemMessage(Component.literal("Please navigate to opposite corner of lot..."));
            }

            //get second bound
            else if (clicks == 1) {
                clicks++;
                lotBounds.add(clickedBlock);
                player.sendSystemMessage(Component.literal("Second bound: (" + clickedBlock.getX() + ", " + clickedBlock.getZ() + ")"));
                player.sendSystemMessage(Component.literal("Please navigate to block before front door..."));

                minXOfLot = Math.min(lotBounds.getFirst().getX(), lotBounds.getLast().getX());
                maxXOfLot = Math.max(lotBounds.getFirst().getX(), lotBounds.getLast().getX());
                minZOfLot = Math.min(lotBounds.getFirst().getZ(), lotBounds.getLast().getZ());
                maxZOfLot = Math.max(lotBounds.getFirst().getZ(), lotBounds.getLast().getZ());
                sqftOfLot = (int) (((maxXOfLot - minXOfLot + 1) * (maxZOfLot - minZOfLot +1)) * SQFT_MULTIPLIER);
            }

            //get block below front door
            else {

                origin = clickedBlock;

                try {

                    perimeter = tracePerimeter();
                    traverseHouse();
                    printResults();


                } catch (Exception e) {
                    player.sendSystemMessage(Component.literal(e.toString()));
                }

                clicks = 0;

                /*
                ItemStack bookStack = new ItemStack(Items.WRITTEN_BOOK);

                if (!player.addItem(bookStack)) {
                    player.drop(bookStack, false);
                }
                */

            }
        }
        return InteractionResult.SUCCESS;
    }


    private static List<BlockPos> tracePerimeter() throws Exception {

        List<BlockPos> result = new ArrayList<>();
        Set<BlockPos> closedList = new HashSet<>();
        houseFacing = getHouseDirection();
        BlockPos topOfDoor = findTopOfDoor();
        BlockPos curr = topOfDoor;
        int currentDirection = findStartingTraceDirection();
        int[][] DIRECTIONS = {{0, 1}, {-1, 1}, {-1, 0}, {-1, -1}, {0, -1}, {1, -1}, {1, 0}, {1, 1}};

        // While current position is not front door, locate next wall block
        do {
            player.sendSystemMessage(Component.literal(result.size() + ": " + curr));
            result.add(curr);
            closedList.add(curr);
            boolean nextWallFound = false;

            for (int i = 0; i < DIRECTIONS.length; i++) {

                // Begin search for next wall block by checking to the left of direction previously moved in
                // Ensures algorithm traces the outermost perimeter (clockwise)
                int newDirection = (currentDirection + 7 + i) % DIRECTIONS.length;
                BlockPos newPos = new BlockPos(curr.getX() + DIRECTIONS[newDirection][0],
                        curr.getY(),
                        curr.getZ() + DIRECTIONS[newDirection][1]);

                // If newPos meets requirements, break out of for loop and add newPos to perimeter
                if (!closedList.contains(newPos) && isWall(newPos) && isInBounds(newPos)) {
                    curr = newPos;
                    currentDirection = newDirection;
                    nextWallFound = true;
                    break;
                }
            }

            // Break out of loop
            if (!nextWallFound) {
                break;
            }
        } while (!(curr.equals(topOfDoor)));

        return result;
    }


    private static void traverseHouse() {

        Stack<BlockPos> stack = new Stack<>();
        Stack<BlockPos> newRoomEntrances = new Stack<>();
        closedList.clear();
        int blocksInRoomCount = 0;
        boolean roomHasBed = false;
        boolean hasCraftingTable = false;
        boolean hasCookingDevice = false;
        boolean hasBed = false;
        blocksInHouse = 0;
        rooms = 0;
        bedrooms = 0;

        // Define source block (src) and push it to stack
        BlockPos src = adjustOriginToInsideHouse();
        src = adjustYPos(src);
        stack.push(src);
        closedList.add(src);

        while (!stack.isEmpty()) {
            BlockPos curr = stack.pop();
            blocksInHouse++;
            blocksInRoomCount++;
            player.sendSystemMessage(Component.literal(blocksInHouse + ": " + curr));

            // Attempt to traverse upwards

            if (canClimbUp(curr) ||
                    (!canClimbUp(curr) && canClimbUp(curr.above()) && canStandOn(curr.below()))) {
                stack.push(curr.above());
                closedList.add(curr.above());
                if (canClimbUp(curr.above()) || !canClimbUp(curr.below()) ||
                        (!canClimbUp(curr.above()) && canClimbUp(curr.above(2)) && canStandOn(curr))) {
                    blocksInHouse--;
                    blocksInRoomCount--;
                }
            }

            // Attempt to traverse downwards
            else if (canClimbDown(curr)) {
                stack.push(curr.below());
                closedList.add(curr.below());
                if (canClimbDown(curr.below())) {
                    blocksInHouse--;
                    blocksInRoomCount--;
                }
            }

            // Traverse in each horizontal direction (north, south, east, west)
            for (Direction dir : DIRECTIONS) {

                // Define new position to explore
                BlockPos newPos = curr.relative(dir);
                newPos = adjustYPos(newPos);

                // Skip if position was already explored
                if (closedList.contains(newPos)) {
                    continue;
                }
                closedList.add(newPos);

                // Skip if position is on perimeter of house
                if (isOnPerimeter(newPos)) {
                    continue;
                }

                if (newPos.getX() == 9 && newPos.getZ() == 12) {
                    player.sendSystemMessage(Component.literal("CURR: " + curr));
                    player.sendSystemMessage(Component.literal("POS: " + newPos));
                    player.sendSystemMessage(Component.literal("canFitAt: " + canFitAt(newPos)));
                    player.sendSystemMessage(Component.literal("isLivableSpace: " + isLivableSpace(newPos, curr)));
                    player.sendSystemMessage(Component.literal("canMoveTo: " + canMoveTo(newPos, curr)));
                }

                // Skip if position does not contain enough space for player
                if (!canFitAt(newPos)) {
                    // Check if position contains livable space. If so, add to square footage
                    if (isLivableSpace(newPos, curr)) {
                        blocksInHouse++;
                        blocksInRoomCount++;
                        player.sendSystemMessage(Component.literal(blocksInHouse + " (LIVABLE): " + newPos));
                    }
                    continue;
                }

                // Skip if player cannot move to position from previous position
                if (!canMoveTo(newPos, curr)) {
                    // Allow algorithm to test if player can move to position from a different previous position
                    closedList.remove(newPos);
                    continue;
                }

                // If position is a potential room entrance, push it to separate stack (to evaluate later) and skip
                if (isDoor(newPos) ||
                        canClimbUp(newPos) || canClimbUp(newPos.above()) ||
                        canClimbDown(newPos)) {
                    newRoomEntrances.push(newPos);
                    player.sendSystemMessage(Component.literal("Pushing " + newPos + " to newRoomEntrances"));
                    continue;
                }

                // If position passes all tests but has different Y-coordinate than previous position,
                // push it to separate stack (to evaluate later as a potential room entrance) and skip
                if (newPos.getY() != curr.getY()) { //NOTE: consider changing to whether difference >= 1.0
                    newRoomEntrances.push(newPos);
                    player.sendSystemMessage(Component.literal("Pushing " + newPos + " to newRoomEntrances"));
                    continue;
                }

                if (isBed(newPos) && !isBedObstructed(newPos)) {
                    hasBed = true;
                    roomHasBed = true;
                }

                if (isCookingDevice(newPos)) {
                    hasCookingDevice = true;
                }

                if (isCraftingTable(newPos)) {
                    hasCraftingTable = true;
                }

                stack.push(newPos);

            }

            // If there are no more blocks to evaluate in current room, take entrance into potential new room
            if (stack.isEmpty() && !newRoomEntrances.isEmpty()) {
                player.sendSystemMessage(Component.literal("(" + blocksInRoomCount + ")"));
                player.sendSystemMessage(Component.literal("Taking entrance"));
                BlockPos entrance = newRoomEntrances.pop();
                stack.push(entrance);
                // If entrance leads both up and down, reevaluate it to analyze second direction
                if (canClimbUp(entrance) && canClimbDown(entrance)) {
                    newRoomEntrances.add(entrance);
                    blocksInHouse--;
                    blocksInRoomCount--;
                }
                // Determine if area around position qualifies as a room/bedroom
                if (blocksInRoomCount >= MIN_BLOCKS_IN_ROOM) {
                    addRoom(roomHasBed);
                    blocksInRoomCount = 0;
                    roomHasBed = false;
                }
            }
        }

        // Determine if area around final position qualifies as a room/bedroom
        if (blocksInRoomCount >= MIN_BLOCKS_IN_ROOM) {
            player.sendSystemMessage(Component.literal("(" + blocksInRoomCount + ")"));
            addRoom(roomHasBed);
        }

        // Update variables
        sqftOfHouse = (int)(blocksInHouse * SQFT_MULTIPLIER);
        isFurnished = hasBed && hasCookingDevice && hasCraftingTable;

    }

    private static void printResults() {
        player.sendSystemMessage(Component.literal("--------------------------------"));
        player.sendSystemMessage(Component.literal("Square Feet (House): " + sqftOfHouse +
                " (" + blocksInHouse + " blocks)"));
        player.sendSystemMessage(Component.literal("Square Feet (Lot): " + sqftOfLot));
        player.sendSystemMessage(Component.literal("Direction: " + houseFacing));
        player.sendSystemMessage(Component.literal("Furnished: " + (isFurnished ? "yes" : "no")));
        player.sendSystemMessage(Component.literal("Rooms: " + rooms));
        player.sendSystemMessage(Component.literal("Bedrooms: " + bedrooms));
        player.sendSystemMessage(Component.literal("--------------------------------"));
    }


    private static Direction getHouseDirection() throws Exception {
        for (Direction dir : DIRECTIONS) {
            if (isDoor(origin.relative(dir)) || isDoor(origin.relative(dir).above())) {
                return dir.getOpposite();
            }
        }
        throw new RuntimeException("Failed to locate front door!");
    }

    private static BlockPos findTopOfDoor() {
        BlockPos topOfDoor = origin.relative(houseFacing.getOpposite());
        if (!isDoor(topOfDoor)) {
            topOfDoor = topOfDoor.above();
        }
        topOfDoor = topOfDoor.above();
        return topOfDoor;

    }

    private static int findStartingTraceDirection() {
        if (houseFacing == Direction.SOUTH) {
            return 0;
        }
        if (houseFacing == Direction.WEST) {
            return 2;
        }
        if (houseFacing == Direction.NORTH) {
            return 4;
        }
        return 6;
    }

    private static BlockPos adjustOriginToInsideHouse() {
        return origin.relative(houseFacing.getOpposite(), 2).above();
    }

    private static BlockPos adjustYPos(BlockPos pos) {
        while (pos.getY() > -64 && !isImpenetrableStandingSurface(pos) && !isClimbable(pos)) {
            pos = pos.below();
        }
        if (isImpenetrableStandingSurface(pos) && !isBed(pos) && maxOnAxis(pos, Direction.Axis.Y) > 0.5) {
            pos = pos.above();
        }
        return pos;
    }

    private static boolean isWall(BlockPos pos) {
        BlockState blockAtPos = level.getBlockState(pos);
        return !(blockAtPos.isAir() || blockAtPos.is(Blocks.WATER)) || blockAtPos.is(Blocks.LAVA);
    }

    private static boolean isInBounds(BlockPos block) {
        return (block.getX() >= minXOfLot && block.getX() <= maxXOfLot &&
                block.getZ() >= minZOfLot && block.getZ() <= maxZOfLot);
    }

    private static boolean isOnPerimeter(BlockPos newPos) {
        return perimeter.stream().anyMatch(p -> p.getX() == newPos.getX() && p.getZ() == newPos.getZ());
    }

    private static boolean isImpenetrableStandingSurface(BlockPos pos) {
        return !isScaffolding(pos) && !isTrapdoor(pos) &&
                spanOnAxis(pos, Direction.Axis.X) >= .875 &&
                spanOnAxis(pos, Direction.Axis.Z) >= .875;
    }

    private static boolean canStandOn(BlockPos pos) {
        return isScaffolding(pos) || isTrapdoor(pos) ||
                (spanOnAxis(pos, Direction.Axis.X) >= .875 &&
                spanOnAxis(pos, Direction.Axis.Z) >= .875);
    }

    private static boolean canFitAt(BlockPos pos, double minVerticalSpace) {
        //If player can fit horizontally in block, return true
        if ((spanOnAxis(pos, Direction.Axis.X) <= (1 - PLAYER_WIDTH) &&
                spanOnAxis(pos.above(), Direction.Axis.X) <= (1 - PLAYER_WIDTH)) ||
                (spanOnAxis(pos, Direction.Axis.Z) <= (1 - PLAYER_WIDTH) &&
                spanOnAxis(pos.above(), Direction.Axis.Z) <= (1 - PLAYER_WIDTH))) {
            return true;
        }
        //Otherwise, if there is adequate vertical space or player is standing on bed, return true
        double ceilingAtPos = ceilingAt(pos, 1);
        double floorAtPos = !isBed(pos) ? floorAt(pos) : (int)floorAt(pos);
        return ceilingAtPos - floorAtPos >= minVerticalSpace;
    }

    private static boolean canFitAt(BlockPos pos) {
        return canFitAt(pos, PLAYER_HEIGHT);
    }

    private static boolean isLivableSpace(BlockPos pos, BlockPos curr) {

        double ceilingAtCurr = ceilingAt(curr, 5);

        BlockPos temp = pos;
        double floorAtTemp = floorAt(temp);
        while (floorAtTemp + MIN_VERTICAL_LIVING_SPACE <= ceilingAtCurr) {
            if ((temp != pos && closedList.contains(temp)) || closedList.contains(temp.above())) {
                return false;
            }
            if (canFitAt(temp, MIN_VERTICAL_LIVING_SPACE)) {
                return true;
            }
            temp = temp.above();
            floorAtTemp = floorAt(temp);
        }

        return false;
    }

    private static boolean canMoveTo(BlockPos pos, BlockPos curr) {
        double floorAtCurr = !isBed(curr) ? floorAt(curr) : (int)floorAt(curr);
        double floorAtPos = !isBed(pos) ? floorAt(pos) : (int)floorAt(pos);
        double ceilingAtCurr = ceilingAt(curr, (curr.getY() - pos.getY() + 3));
        double ceilingAtPos = ceilingAt(pos, (curr.getY() - pos.getY() + 2));
        // If player must move up to reach position from previous position
        if (floorAtPos > floorAtCurr) {
            // If player bumps head on block above
            if (ceilingAtCurr - floorAtPos < PLAYER_HEIGHT) {
                return false;
            }
            // If player can jump to position from previous position
            return floorAtPos - floorAtCurr <= MAX_JUMP_HEIGHT;
        }
        else {
            return ceilingAtPos - floorAtCurr >= PLAYER_HEIGHT;
        }
    }

    private static double floorAt(BlockPos pos) {
        return pos.getY() + (isImpenetrableStandingSurface(pos) ? maxOnAxis(pos, Direction.Axis.Y) : 0);
    }

    /*private static double floorAtPlusTrapdoor(BlockPos pos, BlockPos curr) {
        if (isTrapdoor(pos)) {
            BlockState trapdoor = blockAt(pos);
            Half trapdoorHalf = trapdoor.getValue(BlockStateProperties.HALF);
            Direction trapdoorFacing = trapdoor.getValue(BlockStateProperties.HORIZONTAL_FACING);
            Direction playerFacing = Direction.fromDelta(
                    pos.getX() - curr.getX(),
                    0,
                    pos.getZ() - curr.getZ()
            );
            if (trapdoorHalf == Half.TOP && trapdoorFacing == playerFacing) {
                return pos.getY() + 1;
            }
        }
        return floorAt(pos);
    }*/

    private static double ceilingAt(BlockPos pos, int maxReachableHeight) {
        int heightAboveGround = 1;
        while (!isImpenetrableStandingSurface(pos.above()) && heightAboveGround < maxReachableHeight) {
            pos = pos.above();
            heightAboveGround++;
        }
        return pos.above().getY() + (isImpenetrableStandingSurface(pos.above()) ?
                (minOnAxis(pos.above(), Direction.Axis.Y) < 1 ? minOnAxis(pos.above(), Direction.Axis.Y) : 1) : 1);
    }

    /*private static double ceilingAtMinusTrapdoor(BlockPos pos, BlockPos curr, int maxReachableHeight) {
        int heightAboveGround = 1;
        while (!isImpenetrableStandingSurface(pos.above()) && !isTrapdoor(pos.above()) &&
                heightAboveGround < maxReachableHeight) {
            pos = pos.above();
            heightAboveGround++;
        }
        if (isTrapdoor(pos.above())) {
            BlockState trapdoor = blockAt(pos.above());
            Half trapdoorHalf = trapdoor.getValue(BlockStateProperties.HALF);
            Direction trapdoorFacing = trapdoor.getValue(BlockStateProperties.HORIZONTAL_FACING);
            Direction playerFacing = Direction.fromDelta(
                    pos.getX() - curr.getX(),
                    0,
                    pos.getZ() - curr.getZ()
            );
            if (trapdoorHalf == Half.BOTTOM && trapdoorFacing == playerFacing) {
                return pos.above().getY();
            }
        }
        return pos.above().getY() + (isImpenetrableStandingSurface(pos.above()) ?
                (minOnAxis(pos.above(), Direction.Axis.Y) < 1 ? minOnAxis(pos.above(), Direction.Axis.Y) : 1) : 1);
    }*/

    private static double maxOnAxis(BlockPos pos, Direction.Axis axis) {
        BlockState state = level.getBlockState(pos);
        VoxelShape shape = state.getCollisionShape(level, pos);
        return (shape.max(axis) > 0 && shape.max(axis) <= 1) ? shape.max(axis) : 0;
    }

    private static double minOnAxis(BlockPos pos, Direction.Axis axis) {
        BlockState state = level.getBlockState(pos);
        VoxelShape shape = state.getCollisionShape(level, pos);
        return (shape.min(axis) >= 0 && shape.min(axis) < 1) ? shape.min(axis) : 1;
    }

    private static double spanOnAxis(BlockPos pos, Direction.Axis axis) {
        return maxOnAxis(pos, axis)-minOnAxis(pos, axis);
    }

    private static boolean isBed(BlockPos pos) {
        BlockState blockAtPos = level.getBlockState(pos);
        return blockAtPos.isBed(level, pos, player);
    }

    private static boolean isBedObstructed(BlockPos pos) {
        BlockState blockAtPos = level.getBlockState(pos);
        Direction bedDirection = blockAtPos.getBedDirection(level, pos);
        BedPart bedPart = blockAtPos.getValue(BedBlock.PART);
        BlockPos otherHalfOfBed = (bedPart == BedPart.FOOT) ? pos.relative(bedDirection) : pos.relative(bedDirection.getOpposite());
        BlockState blockAbovePos = level.getBlockState(pos.above());
        BlockState blockAboveOtherBedHalf = level.getBlockState(otherHalfOfBed.above());
        return blockAbovePos.isSolidRender(level, pos) ||
                blockAboveOtherBedHalf.isSolidRender(level, pos);
    }

    private static boolean isCookingDevice(BlockPos pos) {
        BlockState blockBelowPos = level.getBlockState(pos.below());
        return blockBelowPos.is(Blocks.FURNACE) ||
                blockBelowPos.is(Blocks.SMOKER) ||
                blockBelowPos.is(Blocks.BLAST_FURNACE);
    }

    private static boolean isCraftingTable(BlockPos pos) {
        BlockState blockBelowPos = level.getBlockState(pos.below());
        return blockBelowPos.is(Blocks.CRAFTING_TABLE);
    }

    private static boolean isClimbable(BlockPos pos) {
        BlockState blockAtPos = level.getBlockState(pos);
        return blockAtPos.is(Blocks.LADDER) ||
                blockAtPos.is(Blocks.VINE) ||
                blockAtPos.is(Blocks.TWISTING_VINES) ||
                blockAtPos.is(Blocks.WEEPING_VINES) ||
                blockAtPos.is(Blocks.WATER) ||
                isScaffolding(pos) ||
                isTrapdoor(pos);
    }

    private static boolean canClimbUp(BlockPos pos) {
        return !closedList.contains(pos.above()) && isClimbable(pos) && canFitAt(pos.above());
    }

    private static boolean canClimbDown(BlockPos pos) {
        BlockState blockBelowPos = level.getBlockState(pos.below());
        return !closedList.contains(pos.below()) && canFitAt(pos.below()) &&
                (isClimbable(pos.below()) || blockBelowPos.isAir());
    }

    private static void addRoom(boolean roomHasBed) {
        rooms++;
        if (roomHasBed) {
            bedrooms++;
        }
    }

    private static boolean isDoor(BlockPos pos) {

        BlockState target = level.getBlockState(pos);
        return target.is(Blocks.ACACIA_DOOR) ||
                target.is(Blocks.BAMBOO_DOOR) ||
                target.is(Blocks.BIRCH_DOOR) ||
                target.is(Blocks.CHERRY_DOOR) ||
                target.is(Blocks.COPPER_DOOR) ||
                target.is(Blocks.CRIMSON_DOOR) ||
                target.is(Blocks.DARK_OAK_DOOR) ||
                target.is(Blocks.EXPOSED_COPPER_DOOR) ||
                target.is(Blocks.IRON_DOOR) ||
                target.is(Blocks.JUNGLE_DOOR) ||
                target.is(Blocks.MANGROVE_DOOR) ||
                target.is(Blocks.OAK_DOOR) ||
                target.is(Blocks.OXIDIZED_COPPER_DOOR) ||
                target.is(Blocks.SPRUCE_DOOR) ||
                target.is(Blocks.WARPED_DOOR) ||
                target.is(Blocks.WAXED_COPPER_DOOR) ||
                target.is(Blocks.WAXED_EXPOSED_COPPER_DOOR) ||
                target.is(Blocks.WAXED_OXIDIZED_COPPER_DOOR) ||
                target.is(Blocks.WAXED_WEATHERED_COPPER_DOOR) ||
                target.is(Blocks.WEATHERED_COPPER_DOOR);
    }

    private static boolean isTrapdoor(BlockPos pos) {

        BlockState target = level.getBlockState(pos);
        return target.is(Blocks.ACACIA_TRAPDOOR) ||
                target.is(Blocks.BAMBOO_TRAPDOOR) ||
                target.is(Blocks.BIRCH_TRAPDOOR) ||
                target.is(Blocks.CHERRY_TRAPDOOR) ||
                target.is(Blocks.COPPER_TRAPDOOR) ||
                target.is(Blocks.CRIMSON_TRAPDOOR) ||
                target.is(Blocks.DARK_OAK_TRAPDOOR) ||
                target.is(Blocks.EXPOSED_COPPER_TRAPDOOR) ||
                target.is(Blocks.IRON_TRAPDOOR) ||
                target.is(Blocks.JUNGLE_TRAPDOOR) ||
                target.is(Blocks.MANGROVE_TRAPDOOR) ||
                target.is(Blocks.OAK_TRAPDOOR) ||
                target.is(Blocks.OXIDIZED_COPPER_TRAPDOOR) ||
                target.is(Blocks.SPRUCE_TRAPDOOR) ||
                target.is(Blocks.WARPED_TRAPDOOR) ||
                target.is(Blocks.WAXED_COPPER_TRAPDOOR) ||
                target.is(Blocks.WAXED_EXPOSED_COPPER_TRAPDOOR) ||
                target.is(Blocks.WAXED_OXIDIZED_COPPER_TRAPDOOR) ||
                target.is(Blocks.WAXED_WEATHERED_COPPER_TRAPDOOR) ||
                target.is(Blocks.WEATHERED_COPPER_TRAPDOOR);
    }

    private static boolean isScaffolding(BlockPos pos) {
        BlockState blockAtPos = level.getBlockState(pos);
        return blockAtPos.is(Blocks.SCAFFOLDING);
    }

    /*
    private static boolean isSlab(BlockPos pos) {

        BlockState target = level.getBlockState(pos);
        return target.is(Blocks.SANDSTONE_SLAB) ||
                target.is(Blocks.ACACIA_SLAB) ||
                target.is(Blocks.ANDESITE_SLAB) ||
                target.is(Blocks.BIRCH_SLAB) ||
                target.is(Blocks.BAMBOO_SLAB) ||
                target.is(Blocks.BAMBOO_MOSAIC_SLAB) ||
                target.is(Blocks.BLACKSTONE_SLAB) ||
                target.is(Blocks.BRICK_SLAB) ||
                target.is(Blocks.CHERRY_SLAB) ||
                target.is(Blocks.COBBLED_DEEPSLATE_SLAB) ||
                target.is(Blocks.JUNGLE_SLAB) ||
                target.is(Blocks.POLISHED_GRANITE_SLAB);
    }

    private static boolean isStairs(BlockPos pos) {

        BlockState target = level.getBlockState(pos);
        return target.is(Blocks.JUNGLE_STAIRS) ||
                target.is(Blocks.POLISHED_GRANITE_STAIRS);
    }
    */

}