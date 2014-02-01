/*
 * ----------------------------------------------------------------------------
 * "THE BEER-WARE LICENSE"
 * As long as you retain this notice you can do whatever you want with this
 * stuff. If you meet an employee from Windward some day, and you think this
 * stuff is worth it, you can buy them a beer in return. Windward Studios
 * ----------------------------------------------------------------------------
 */

package net.windward.Windwardopolis2.AI;

import java.awt.Point;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.SortedSet;
import java.util.TreeSet;

import net.windward.Windwardopolis2.TRAP;
import net.windward.Windwardopolis2.api.CoffeeStore;
import net.windward.Windwardopolis2.api.Company;
import net.windward.Windwardopolis2.api.Map;
import net.windward.Windwardopolis2.api.MapSquare;
import net.windward.Windwardopolis2.api.Passenger;
import net.windward.Windwardopolis2.api.Player;
import net.windward.Windwardopolis2.api.PowerUp;

import org.apache.log4j.Logger;

/**
 * The sample C# AI. Start with this project but write your own code as this is a very simplistic implementation of the AI.
 */
public class MyPlayerBrain implements net.windward.Windwardopolis2.AI.IPlayerAI {
    // bugbug - put your team name here.
    private static String NAME = "Mr. Bond";

    // bugbug - put your school name here. Must be 11 letters or less (ie use MIT, not Massachussets Institute of Technology).
    public static String SCHOOL = "Purdue U.";

    private static Logger log = Logger.getLogger(IPlayerAI.class);

    /**
     * The name of the player.
     */
    private String privateName;

    public final String getName() {
        return privateName;
    }

    private void setName(String value) {
        privateName = value;
    }

    /**
     * The game map.
     */
    private Map privateGameMap;

    public final Map getGameMap() {
        return privateGameMap;
    }

    private void setGameMap(Map value) {
        privateGameMap = value;
    }

    /**
     * All of the players, including myself.
     */
    private java.util.ArrayList<Player> privatePlayers;

    public final java.util.ArrayList<Player> getPlayers() {
        return privatePlayers;
    }

    private void setPlayers(java.util.ArrayList<Player> value) {
        privatePlayers = value;
    }

    /**
     * All of the companies.
     */
    private java.util.ArrayList<Company> privateCompanies;

    public final java.util.ArrayList<Company> getCompanies() {
        return privateCompanies;
    }

    private void setCompanies(java.util.ArrayList<Company> value) {
        privateCompanies = value;
    }

    /**
     * All of the passengers.
     */
    private java.util.ArrayList<Passenger> privatePassengers;

    public final java.util.ArrayList<Passenger> getPassengers() {
        return privatePassengers;
    }

    private void setPassengers(java.util.ArrayList<Passenger> value) {
        privatePassengers = value;
    }

    /**
     * All of the coffee stores.
     */
    private java.util.ArrayList<CoffeeStore> privateStores;

    public final ArrayList<CoffeeStore> getCoffeeStores() { return privateStores; }

    private void setCoffeeStores(ArrayList<CoffeeStore> value) { privateStores = value; }

    /**
     * The power up deck
     */
    private ArrayList<PowerUp> privatePowerUpDeck;

    public final ArrayList<PowerUp> getPowerUpDeck() { return privatePowerUpDeck; }

    private void setPowerUpDeck(ArrayList<PowerUp> value) { privatePowerUpDeck = value; }


    /**
     * My power up hand
     */
    private ArrayList<PowerUp> privatePowerUpHand;

    public final ArrayList<PowerUp> getPowerUpHand() { return privatePowerUpHand; }

    private void setPowerUpHand(ArrayList<PowerUp> value) { privatePowerUpHand = value; }

    /**
     * Me (my player object).
     */
    private Player privateMe;

    public final Player getMe() {
        return privateMe;
    }

    private void setMe(Player value) {
        privateMe = value;
    }

    /**
     * My current passenger
     */
    private Passenger privateMyPassenger;

    public final Passenger getMyPassenger() { return privateMyPassenger; }

    private void setMyPassenger(Passenger value) { privateMyPassenger = value; }

    private PlayerAIBase.STATUS pendingStatus;
    
    private PlayerAIBase.PlayerOrdersEvent sendOrders;

    private PlayerAIBase.PlayerCardEvent playCards;
    
    /**
     * The maximum number of trips allowed before a refill is required.
     */
    private static final int MAX_TRIPS_BEFORE_REFILL = 3;

    private static final java.util.Random rand = new java.util.Random();

    public MyPlayerBrain(String name) {
        setName(!net.windward.Windwardopolis2.DotNetToJavaStringHelper.isNullOrEmpty(name) ? name : NAME);
        privatePowerUpHand = new ArrayList<PowerUp>();
    }

    /**
     * The avatar of the player. Must be 32 x 32.
     */
    public final byte[] getAvatar() {
        try {
            // open image
            InputStream stream = getClass().getResourceAsStream("/net/windward/Windwardopolis2/res/MyAvatar.png");

            byte [] avatar = new byte[stream.available()];
            stream.read(avatar, 0, avatar.length);
            return avatar;

        } catch (IOException e) {
            System.out.println("error reading image");
            e.printStackTrace();
            return null;
        }
    }


    /**
     * Called at the start of the game.
     *
     * @param map         The game map.
     * @param me          You. This is also in the players list.
     * @param players     All players (including you).
     * @param companies   The companies on the map.
     * @param passengers  The passengers that need a lift.
     * @param ordersEvent Method to call to send orders to the server.
     */
    public final void Setup(Map map, Player me, java.util.ArrayList<Player> players, java.util.ArrayList<Company> companies, ArrayList<CoffeeStore> stores,
                            java.util.ArrayList<Passenger> passengers, ArrayList<PowerUp> powerUps, PlayerAIBase.PlayerOrdersEvent ordersEvent, PlayerAIBase.PlayerCardEvent cardEvent) {

        try {
            setGameMap(map);
            setPlayers(players);
            setMe(me);
            setCompanies(companies);
            setPassengers(passengers);
            setCoffeeStores(stores);
            setPowerUpDeck(powerUps);
            sendOrders = ordersEvent;
            playCards = cardEvent;

            java.util.ArrayList<Passenger> pickup = AllPickups(me, players, passengers);

            // get the path from where we are to the dest.
            java.util.ArrayList<Point> path = CalculatePathPlus1(me, pickup.get(0).getLobby().getBusStop());
            sendOrders.invoke("ready", path, pickup);
        } catch (RuntimeException ex) {
            log.fatal("setup(" + me == null ? "NULL" : me.getName() + ") Exception: " + ex.getMessage());
            ex.printStackTrace();

        }
    }
    
    
	private Passenger passengerHunting = null;	// The passenger we are currently GOING for
	private String prevHuntingDmesg = null;
    /**
     * Called to send an update message to this A.I. We do NOT have to send orders in response.
     *
     * @param status     The status message.
     * @param plyrStatus The player this status is about. THIS MAY NOT BE YOU.
     */
    public final void GameStatus(PlayerAIBase.STATUS status, Player plyrStatus) {

        // bugbug - Framework.cs updates the object's in this object's Players, Passengers, and Companies lists. This works fine as long
        // as this app is single threaded. However, if you create worker thread(s) or respond to multiple status messages simultaneously
        // then you need to split these out and synchronize access to the saved list objects.

        try {
//        	ArrayList<Player> players = new ArrayList<Player>(getPlayers());	
//        	for (int i = 0; i < players.size(); i++) {
//        		if (players.get(i).equals(plyrStatus)) {
//        			players.set(i, plyrStatus);
//        			privatePlayers = players;
//        			break;
//        		}
//        	}
        	
//        	log.info(getPlayers().toString());
//        	System.out.println("PLAYERS: " + getPlayers());
//        	System.out.println("PASSENGERS: " + getPassengers());
        	
        	Point ptDest = null;
            java.util.ArrayList<Passenger> pickup = new java.util.ArrayList<Passenger>();
            ArrayList<Point> path = null;
        	
            if (getMe().getLimo().getPassenger() == null && getMe().getLimo().getCoffeeServings()>0) {
            	pickup = AllPickups(getMe(), getPlayers(), getPassengers());
            	ptDest = pickup.get(0).getLobby().getBusStop();
            	passengerHunting = pickup.get(0);
            }
            
        	// bugbug - we return if not us because the below code is only for when we need a new path or our limo hit a bus stop.
            // if you want to act on other players arriving at bus stops, you need to remove this. But make sure you use Me, not
            // plyrStatus for the Player you are updatiing (particularly to determine what tile to start your path from).
            if (!plyrStatus.equals(getMe())) {
            	switch (status) {
            	case PASSENGER_PICKED_UP:
            		if(plyrStatus.getLimo().getPassenger()!=null && passengerHunting!=null){
            			if(plyrStatus.getLimo().getPassenger().equals(passengerHunting)){
            				System.out.println(plyrStatus.getName() + " picked up " + plyrStatus.getLimo().getPassenger().getName());
            				pickup = AllPickups(getMe(), getPlayers(), getPassengers());
            				ptDest = pickup.get(0).getDestination().getBusStop();
            				passengerHunting = pickup.get(0);
            				System.out.println("Pickup (0): " + pickup.get(0).getName());
            				// DMESG
            				System.out.println();
            			}
            		}
            	}
            	if (pendingStatus != null) {
					switch (pendingStatus) {
					case PASSENGER_REFUSED_ENEMY:
						// DMESG
						System.out.println("PENDING_REFUSED: " + getMe().getLimo().getPassenger().getName());

						passengerHunting = null;	// When reach this condition, must having a passenger on board
						pickup = AllPickups(getMe(), getPlayers(), getPassengers());
						path = whenRefused(getMe(), getPlayers(), getPassengers(), pickup);
						// After calling whenRefused, might going to hunt another passenger

						// DMESG
						System.out.println();
						break;
					default:
						break;
					}
				}

//				if (getMe().getLimo().getPassenger() == null) {
//					pickup = AllPickups(getMe(), getPlayers(), getPassengers());
//					ptDest = pickup.get(0).getLobby().getBusStop();
//					passengerHunting = pickup.get(0);
//				}
            } else {
                if(status == PlayerAIBase.STATUS.UPDATE) {
                    MaybePlayPowerUp();
                    
                    int K_COFFEE_REFILL_PATH_RATIO_MAX = 6;
                    
                    Point cfNear =  getNearestCoffeeStore(getMe()).getBusStop();
                    
                    double quarterRoadSize = countRoundSize() / (double) 16;
                    ArrayList<Point> cfPath = CalculatePathPlus1(getMe(), cfNear);
                    
                    double cfPathRatio = cfPath.size() / quarterRoadSize;
                    
                    if (getMe().getLimo().getPassenger() == null &&
                    		getMe().getLimo().getCoffeeServings() == 1 &&
                    		cfPathRatio <= K_COFFEE_REFILL_PATH_RATIO_MAX) {
                    	pickup = null;
                    	ptDest = cfNear;
                    	passengerHunting = null;
                    }
                    
                    if (ptDest != null) {
                    	DisplayOrders(ptDest);
                    	path = CalculatePathPlus1(getMe(), ptDest);
                    	sendOrders.invoke("move", path, pickup);
                    }

                    return;
                }

                DisplayStatus(status, plyrStatus);

                if(log.isDebugEnabled())
                    log.info("gameStatus( " + status + " )");

    			switch (status) {
//    			case UPDATE:
//    				if (pendingStatus != null) {
//    					switch (pendingStatus) {
//    					case PASSENGER_REFUSED:
//    						// DMESG
//    						System.out.println("PENDING_REFUSED: " + getMe().getLimo().getPassenger().getName());
    //
//    						passengerHunting = null;	// When reach this condition, must having a passenger on board
//    						pickup = AllPickups(getMe(), getPlayers(), getPassengers());
//    						path = whenRefused(getMe(), getPlayers(), getPassengers(), pickup);
//    						// After calling whenRefused, might going to hunt another passenger
    //
//    						// DMESG
//    						System.out.println();
//    						break;
//    					default:
//    						break;
//    					}
//    				}
    //
//    				if (getMe().getLimo().getPassenger() == null) {
//    					pickup = AllPickups(getMe(), getPlayers(), getPassengers());
//    					ptDest = pickup.get(0).getLobby().getBusStop();
//    					passengerHunting = pickup.get(0);
//    				}
//    				break;
    			case NO_PATH:
    				// DMESG
    				System.out.println("NO_PATH");

//    				if (getMe().getLimo().getPassenger() == null) {
//    					pickup = AllPickups(getMe(), getPlayers(), getPassengers());
//    					ptDest = pickup.get(0).getLobby().getBusStop();
//    					passengerHunting = pickup.get(0);
//    				} else {
//    					ptDest = getMe().getLimo().getPassenger().getDestination().getBusStop();
//    				}
    				if (getMe().getLimo().getPassenger() == null) {
    					pickup = AllPickups(getMe(), getPlayers(), getPassengers());

    					// TODO: Intended for preventing picking up loop
    					// But seems a little bit buggy?
//    					if (passengerHunting != null) {
//    						for (Passenger pp : pickup) {
//    							if (!pp.equals(passengerHunting)) {
//    								ptDest = pp.getLobby().getBusStop();
//    								passengerHunting = pp;
//    							}
//    						}
//    					} else {
    						ptDest = pickup.get(0).getLobby().getBusStop();
    						passengerHunting = pickup.get(0);
//    					}
    				} else {
    					ptDest = getMe().getLimo().getPassenger().getDestination().getBusStop();
    				}

    				// DMESG
    				System.out.println();

    				break;
    			case PASSENGER_NO_ACTION:
    				// DMESG
    				System.out.println("NO_ACTION");

    				if (getMe().getLimo().getPassenger() == null) {
    					pickup = AllPickups(getMe(), getPlayers(), getPassengers());

    					// TODO: Intended for preventing picking up loop
    					// But seems a little bit buggy?
//    					if (passengerHunting != null) {
//    						for (Passenger pp : pickup) {
//    							if (!pp.equals(passengerHunting)) {
//    								ptDest = pp.getLobby().getBusStop();
//    								passengerHunting = pp;
//    							}
//    						}
//    					} else {
    						ptDest = pickup.get(0).getLobby().getBusStop();
    						passengerHunting = pickup.get(0);
//    					}
    				} else {
    					ptDest = getMe().getLimo().getPassenger().getDestination().getBusStop();
    				}

    				// DMESG
    				System.out.println();

    				break;
    			case PASSENGER_DELIVERED:
    			case PASSENGER_ABANDONED:
    				pendingStatus = status;

    				// DMESG
    				System.out.println("DELIVERED/ABANDONED (NO_PICK_UP)");

    				passengerHunting = null;
    				pickup = AllPickups(getMe(), getPlayers(), getPassengers());
    				ptDest = pickup.get(0).getLobby().getBusStop();
    				passengerHunting = pickup.get(0);

    				// DMESG
    				System.out.println();

    				break;
    			case PASSENGER_REFUSED_ENEMY:
    				pendingStatus = status;

    				// DMESG
    				System.out.println("REFUSED: " + getMe().getLimo().getPassenger().getName());

    				passengerHunting = null;	// When reach this condition, must having a passenger on board
    				pickup = AllPickups(getMe(), getPlayers(), getPassengers());
    				path = whenRefused(getMe(), getPlayers(), getPassengers(), pickup);
    				// After calling whenRefused, might going to hunt another passenger

    				// DMESG
    				System.out.println();

    				break;
    			case PASSENGER_DELIVERED_AND_PICKED_UP:
    				// DMESG
    				System.out.println("DELIVERED");

    			case PASSENGER_PICKED_UP:
    				pendingStatus = status;

    				// DMESG
    				System.out.println("PICKED_UP: "
    						+ getMe().getLimo().getPassenger().getName()
    						+ " TOWARDS "
    						+ getMe().getLimo().getPassenger().getDestination().getName());

    				passengerHunting = null;
    				pickup = AllPickups(getMe(), getPlayers(), getPassengers());
    				ptDest = getMe().getLimo().getPassenger().getDestination()
    						.getBusStop();

    				// DMESG
    				System.out.println();

    				break;
    			}

                // coffee store override
                switch (status)
                {
                    case PASSENGER_DELIVERED_AND_PICKED_UP:
                    case PASSENGER_DELIVERED:
                    case PASSENGER_ABANDONED:
            			if (getMe().getLimo().getPassenger() == null) {
            				// No passenger, coffee!!
            				if (getMe().getLimo().getCoffeeServings() == 0) {
            					ptDest = getNearestCoffeeStore(getMe()).getBusStop();
            				}
                        }
                        break;
                    case PASSENGER_REFUSED_NO_COFFEE:
                    case PASSENGER_DELIVERED_AND_PICK_UP_REFUSED:
                    	ptDest = getNearestCoffeeStore(getMe()).getBusStop();
                        break;
                    case COFFEE_STORE_CAR_RESTOCKED:
                        pickup = AllPickups(getMe(), getPlayers(), getPassengers());
                        if (pickup.size() == 0)
                            break;
                        ptDest = pickup.get(0).getLobby().getBusStop();
                        break;
                }
            }
            if (getMe().getLimo().getPassenger() == null) {
				// No passenger, coffee!!
				if (getMe().getLimo().getCoffeeServings() == 0) {
					ptDest = getNearestCoffeeStore(getMe()).getBusStop();
					passengerHunting = null;
				}
            }
//            // may be another status
//            if(ptDest == null)
//                return;
//
//            DisplayOrders(ptDest);
//
//            // get the path from where we are to the dest.
//            path = CalculatePathPlus1(getMe(), ptDest);
//
//            if (log.isDebugEnabled())
//            {
//                log.debug(status + "; Path:" + (path.size() > 0 ? path.get(0).toString() : "{n/a}") + "-" + (path.size() > 0 ? path.get(path.size()-1).toString() : "{n/a}") + ", " + path.size() + " steps; Pickup:" + (pickup.size() == 0 ? "{none}" : pickup.get(0).getName()) + ", " + pickup.size() + " total");
//            }
//
//            // update our saved Player to match new settings
//            if (path.size() > 0) {
//                getMe().getLimo().getPath().clear();
//                getMe().getLimo().getPath().addAll(path);
//            }
//            if (pickup.size() > 0) {
//                getMe().getPickUp().clear();
//                getMe().getPickUp().addAll(pickup);
//            }
//
//            sendOrders.invoke("move", path, pickup);
            
            DisplayOrders(ptDest);
            
            // DMESG
			String curHuntingDmesg = "HUNTING: " + (passengerHunting != null ? passengerHunting.getName() : "<null>");
			if (prevHuntingDmesg == null || !prevHuntingDmesg.equals(curHuntingDmesg)) {
				System.out.println(curHuntingDmesg);
				prevHuntingDmesg = curHuntingDmesg;
			}

			if (pickup.size() > 0) {
				getMe().getPickUp().clear();
				getMe().getPickUp().addAll(pickup);
			}

			// Accept path as higher priory because of whenRefused function
			// Otherwise, it might cause some bugs (not confirmed)
			if (path != null) {
				// update our saved Player to match new settings
				if (path.size() > 0) {
					getMe().getLimo().getPath().clear();
					getMe().getLimo().getPath().addAll(path);
				}
				
				System.out.println("Sending PATH!");
				System.out.println("path: " + path);
				System.out.println();
				sendOrders.invoke("move", path, pickup);
				return;
			}

			if (ptDest != null) {
				// get the path from where we are to the dest.
				path = CalculatePathPlus1(getMe(), ptDest);

				// update our saved Player to match new settings
				if (path.size() > 0) {
					getMe().getLimo().getPath().clear();
					getMe().getLimo().getPath().addAll(path);
				}
				
				System.out.println("Sending DEST!");
				System.out.println("dest: " + ptDest);
				System.out.println("path: " + path);
				System.out.println();
				sendOrders.invoke("move", path, pickup);
				return;
			}
        } catch (RuntimeException ex) {
            ex.printStackTrace();
        }
    }
    
    private CoffeeStore getNearestCoffeeStore(Player me) {
    	CoffeeStore mcf = null;
    	for (CoffeeStore cf : getCoffeeStores()) {
    		if (mcf == null) {
    			mcf = cf;
    		} else {
    			ArrayList<Point> mcfp = CalculatePathPlus1(me, mcf.getBusStop());
    			ArrayList<Point> cfp = CalculatePathPlus1(me, cf.getBusStop());
    			
    			if (cfp.size() < mcfp.size()) {
    				mcf = cf;
    			}
    		}
    	}
    	
    	return mcf;
    }

    private void MaybePlayPowerUp() {
        if ((getPowerUpHand().size() != 0) && (rand.nextInt(50) < 30))
            return;
        // not enough, draw
        if (getPowerUpHand().size() < getMe().getMaxCardsInHand() && getPowerUpDeck().size() > 0)
        {
            for (int index = 0; index < getMe().getMaxCardsInHand() - getPowerUpHand().size() && getPowerUpDeck().size() > 0; index++)
            {
                // select a card
                PowerUp pu = getPowerUpDeck().get(0);
                privatePowerUpDeck.remove(pu);
                privatePowerUpHand.add(pu);
                playCards.invoke(PlayerAIBase.CARD_ACTION.DRAW, pu);
            }
            return;
        }

        // can we play one?
        PowerUp pu2 = null;
        for(PowerUp current : getPowerUpHand()) {
            if(current.isOkToPlay()) {
                pu2 = current;
                break;
            }
        }

        if (pu2 == null)
            return;
        // 10% discard, 90% play
        if (rand.nextInt(10) == 0)
            playCards.invoke(PlayerAIBase.CARD_ACTION.DISCARD, pu2);
        else
        {
            if (pu2.getCard() == PowerUp.CARD.MOVE_PASSENGER) {
                Passenger toUseCardOn = null;
                for(Passenger pass : privatePassengers) {
                    if(pass.getCar() == null) {
                        toUseCardOn = pass;
                        break;
                    }
                }
                pu2.setPassenger(toUseCardOn);
            }
            if (pu2.getCard() == PowerUp.CARD.CHANGE_DESTINATION || pu2.getCard() == PowerUp.CARD.STOP_CAR)
            {
                java.util.ArrayList<Player> plyrsWithPsngrs = new ArrayList<Player>();
                for(Player play : privatePlayers) {
                    if(play.getGuid() != getMe().getGuid() && play.getLimo().getPassenger() != null) {
                        plyrsWithPsngrs.add(play);
                    }
                }

                if (plyrsWithPsngrs.size() == 0)
                    return;
                pu2.setPlayer(plyrsWithPsngrs.get(0));
            }
            if (log.isInfoEnabled())
                log.info("Request play card " + pu2);
            playCards.invoke(PlayerAIBase.CARD_ACTION.PLAY, pu2);
        }
        privatePowerUpHand.remove(pu2);
    }

    /**
     * A power-up was played. It may be an error message, or success.
     * @param puStatus - The status of the played card.
     * @param plyrPowerUp - The player who played the card.
     * @param cardPlayed - The card played.
     */
    public void PowerupStatus(PlayerAIBase.STATUS puStatus, Player plyrPowerUp, PowerUp cardPlayed)
    {
        // redo the path if we got relocated
        if ((puStatus == PlayerAIBase.STATUS.POWER_UP_PLAYED) && ((cardPlayed.getCard() == PowerUp.CARD.RELOCATE_ALL_CARS) ||
                ((cardPlayed.getCard() == PowerUp.CARD.CHANGE_DESTINATION) && (cardPlayed.getPlayer() != null ? cardPlayed.getPlayer().getGuid() : null) == getMe().getGuid())))
            GameStatus(PlayerAIBase.STATUS.NO_PATH, getMe());
    }

    private void DisplayStatus(PlayerAIBase.STATUS status, Player plyrStatus)
    {
        String msg = null;
        switch (status)
        {
            case PASSENGER_DELIVERED:
                msg = getMyPassenger().getName() + " delivered to " + getMyPassenger().getLobby().getName();
                privateMyPassenger = null;
                break;
            case PASSENGER_ABANDONED:
                msg = getMyPassenger().getName() + " abandoned at " + getMyPassenger().getLobby().getName();
                privateMyPassenger = null;
                break;
            case PASSENGER_REFUSED_ENEMY:
                msg = plyrStatus.getLimo().getPassenger().getName() + " refused to exit at " +
                        plyrStatus.getLimo().getPassenger().getDestination().getName() + " - enemy there";
                break;
            case PASSENGER_DELIVERED_AND_PICKED_UP:
                msg = getMyPassenger().getName() + " delivered at " + getMyPassenger().getLobby().getName() + " and " +
                        plyrStatus.getLimo().getPassenger().getName() + " picked up";
                privateMyPassenger = plyrStatus.getLimo().getPassenger();
                break;
            case PASSENGER_PICKED_UP:
                msg = plyrStatus.getLimo().getPassenger().getName() + " picked up";
                privateMyPassenger = plyrStatus.getLimo().getPassenger();
                break;
            case PASSENGER_REFUSED_NO_COFFEE:
                msg = "Passenger refused to board limo, no coffee";
                break;
            case PASSENGER_DELIVERED_AND_PICK_UP_REFUSED:
                msg = getMyPassenger().getName() + " delivered at " + getMyPassenger().getLobby().getName() +
                        ", new passenger refused to board limo, no coffee";
                break;
            case COFFEE_STORE_CAR_RESTOCKED:
                msg = "Coffee restocked!";
                break;
        }
        if (msg != null && !msg.equals(""))
        {
            System.out.println(msg);
            if (log.isInfoEnabled())
                log.info(msg);
        }
    }

    private void DisplayOrders(Point ptDest)
    {
        String msg = null;
        CoffeeStore store = null;
        for(CoffeeStore s : getCoffeeStores()) {
            if(s.getBusStop() == ptDest) {
                store = s;
                break;
            }
        }

        if (store != null)
            msg = "Heading toward " + store.getName() + " at " + ptDest.toString();
        else
        {
            Company company = null;
            for(Company c : getCompanies()) {
                if(c.getBusStop() == ptDest) {
                    company = c;
                    break;
                }
            }

            if (company != null)
                msg = "Heading toward " + company.getName() + " at " + ptDest.toString();
        }
        if (msg != null && !msg.equals(""))
        {
            System.out.println(msg);
            if (log.isInfoEnabled())
                log.info(msg);
        }
    }
    
	private ArrayList<Point> whenRefused(Player me, ArrayList<Player> players,
			ArrayList<Passenger> passengers, ArrayList<Passenger> pickup) {
		Passenger currentPassenger = me.getLimo().getPassenger();

		boolean enemyHasGone = true;
		for (Passenger passenger : currentPassenger.getDestination().getPassengers()) {
			if (currentPassenger.getEnemies().indexOf(passenger) >= 0) {
				enemyHasGone = false;
				break;
			}
		}

		if (enemyHasGone) {
			// DMESG
			System.out.println("ENEMY GONE");

			passengerHunting = null;	// enemy gone, deliver current passenger, stop hunting
			return CalculatePathPlus1(me, currentPassenger.getDestination().getBusStop());
		}

		Company noWaitCompany = null;
		boolean noWait = true;

		if (pickup.size() > 0) {
			// Pick somebody else
			for (int i = 0; i < pickup.size(); i++) {
				if (currentPassenger.getEnemies().indexOf(pickup.get(i)) < 0) {
					passengerHunting = pickup.get(i);
					noWaitCompany = pickup.get(i).getLobby();

					// DMESG
					System.out.println("pick up: " + pickup.get(i).getName());
					break;
				}
			}
		} else {
			// Dump to closest
			passengerHunting = null;

			// DMESG
			System.out.println("dump to closest");

			Company closestCompany = null;
			for (Company company : getCompanies()) {
				if (!company.equals(currentPassenger.getDestination())) {
					if (closestCompany == null) {
						closestCompany = company;
					}

					if (CalculatePathPlus1(getMe(), company.getBusStop()).size()
							< CalculatePathPlus1(getMe(), closestCompany.getBusStop()).size()) {
						closestCompany = company;
					}
				}
			}
			noWaitCompany = closestCompany;
		}

		// It's a bug if noWaitCompany is null
		if (noWaitCompany == null) {
			TRAP.trap();
		}

		for (Player player : players) {
			if (!player.equals(getMe()) && isPickingUpEnemy(player, currentPassenger)) {
				ArrayList<Point> playerPath = CalculatePathPlus1(player, currentPassenger.getDestination().getBusStop());
				ArrayList<Point> mePath = CalculatePathPlus1(getMe(), noWaitCompany.getBusStop());

				if (playerPath.size() < mePath.size()) {
					noWait = false;
					break;
				}
			}
		}

		if (noWait) {
			// Don't wait, dump current passenger

			// DMESG
			System.out.println("DUMP AT " + noWaitCompany.getName());

			return CalculatePathPlus1(me, noWaitCompany.getBusStop());
		} else {
			// Wander around, wait for enemy leave
			passengerHunting = null;	// just wandering around, definitely not hunting

			// DMESG
			System.out.println("WANDER AROUND");

			ArrayList<Point> totalPath = new ArrayList<Point>();
			ArrayList<Point> singlePath = SimpleAStar.CalculatePath(getGameMap(), getMe().getLimo().getMapPosition(), currentPassenger.getDestination().getBusStop());
			ArrayList<Point> reverseSinglePath = new ArrayList<Point>(singlePath);
			Collections.reverse(reverseSinglePath);
			for (int i = 0; i < 100; i++) {
				// Loop 100 times
				totalPath.addAll(singlePath);
				totalPath.addAll(reverseSinglePath);
			}

			return totalPath;
		}
	}
	
	private boolean isPickingUpEnemy(Player player, Passenger currentPassenger) {
		if (player.getPickUp().size() <= 0) {
			return false;
		}

		Passenger playerFirstPickup = player.getPickUp().get(0);
		if (playerFirstPickup.getLobby() == null) {
			return false;
		}

		boolean isPickingUpEnemy = currentPassenger.getEnemies().indexOf(playerFirstPickup) >= 0;
		if (!isPickingUpEnemy) {
			return false;
		}

		if (player.getLimo().getPassenger() != null) {
			// Has passenger
			// TODO: fix?
			// Assuming players will deliver their passengers to dest
			boolean isPassengerToDest = player.getLimo().getPassenger().equals(currentPassenger.getDestination());
			if (!isPassengerToDest) {
				return false;
			}

			boolean isPassengerEnemy = currentPassenger.getEnemies().indexOf(player.getLimo().getPassenger()) >= 0;
			if (isPassengerEnemy) {
				return false;
			}

			boolean isPassengerRefusing = false;
			List<Passenger> playerPassengerEnemies = player.getLimo().getPassenger().getEnemies();
			for (Passenger waitingPassenger : currentPassenger.getDestination().getPassengers()) {
				if (playerPassengerEnemies.indexOf(waitingPassenger) >= 0) {
					isPassengerRefusing = true;
					break;
				}
			}
			if (isPassengerRefusing) {
				return false;
			}
		} else {
			// No passenger
			boolean isPickingUpAtDest = playerFirstPickup.getLobby().equals(currentPassenger.getDestination());
			if (!isPickingUpAtDest) {
				return false;
			}
		}

		return true;
	}

    private java.util.ArrayList<Point> CalculatePathPlus1(Player me, Point ptDest) {
        java.util.ArrayList<Point> path = SimpleAStar.CalculatePath(getGameMap(), me.getLimo().getMapPosition(), ptDest);
        // add in leaving the bus stop so it has orders while we get the message saying it got there and are deciding what to do next.
        if (path.size() > 1) {
            path.add(path.get(path.size() - 2));
        }
        return path;
    }

//    private static java.util.ArrayList<Passenger> AllPickups(Player me, Iterable<Passenger> passengers) {
//        java.util.ArrayList<Passenger> pickup = new java.util.ArrayList<Passenger>();
//
//        for (Passenger psngr : passengers) {
//            if ((!me.getPassengersDelivered().contains(psngr)) && (psngr != me.getLimo().getPassenger()) && (psngr.getCar() == null) && (psngr.getLobby() != null) && (psngr.getDestination() != null))
//                pickup.add(psngr);
//        }
//
//        //add sort by random so no loops for can't pickup
//        return pickup;
//    }
    private ArrayList<Passenger> AllPickups(Player me,
			ArrayList<Player> players, Iterable<Passenger> passengers) {
		PassengerPrioryComparator pComparator = new PassengerPrioryComparator(me, players);
		ArrayList<Passenger> pickup = new ArrayList<Passenger>();
		SortedSet<Passenger> sortedPickup = new TreeSet<Passenger>(pComparator);

		for (Passenger psngr : passengers) {
			if ((!me.getPassengersDelivered().contains(psngr))
					&& (psngr != me.getLimo().getPassenger())
					&& (psngr.getCar() == null) && (psngr.getLobby() != null)
					&& (psngr.getDestination() != null))
				sortedPickup.add(psngr);
		}

		for (Passenger psngr : sortedPickup) {
			pickup.add(psngr);
		}

		// DMESG
		StringBuilder dmesgSb = new StringBuilder();
		dmesgSb.append("AllPickups: ");
		dmesgSb.append("[");
		for (Passenger pp : pickup) {
			dmesgSb.append(pp.getName());
			dmesgSb.append(" (");
			dmesgSb.append(pComparator.getScore(pp));
			dmesgSb.append("), ");
		}
		dmesgSb.delete(dmesgSb.length() - 2, dmesgSb.length());
		dmesgSb.append("]");
		System.out.println(dmesgSb.toString());

		return pickup;
	}
    
    private class PassengerPrioryComparator implements Comparator<Passenger> {
		private final Player me;
		private final ArrayList<Player> players;

		// Multipliers and constants
		private static final int K_POINTS_DELIVERED = 6;
		private static final int M_DISTANCE_PENALTY = -5;
		private static final int K_OTHERS_AHEAD = -100; // Other players are ahead of us

		public PassengerPrioryComparator(Player me, ArrayList<Player> players) {
			this.me = me;
			this.players = players;
		}

		@Override
		public int compare(Passenger p1, Passenger p2) {
			int p1p = getScore(p1);
			int p2p = getScore(p2);

			// Higher score first
			return p2p - p1p;
		}

		public int getScore(Passenger passenger) {
			int score = 0;

			final ArrayList<Point> pPath = SimpleAStar.CalculatePath(getGameMap(), me.getLimo().getMapPosition(), passenger.getLobby().getBusStop());
			final ArrayList<Point> pPickupDestPath = SimpleAStar.CalculatePath(getGameMap(), passenger.getLobby().getBusStop(), passenger.getDestination().getBusStop());
			// double halfDiagonal = Math.sqrt(Math.pow(getGameMap().getHeight(), 2) + Math.pow(getGameMap().getWidth(), 2)) / 2;
			int quarterRoadSize = countRoundSize() / 4;
			
			// Add deliver points
			score += passenger.getPointsDelivered() + K_POINTS_DELIVERED;

			// Based on current distance and delivery distance
			double baseDistance = quarterRoadSize;
			score += ((pPath.size() + pPickupDestPath.size()) / baseDistance) * M_DISTANCE_PENALTY;

			// Based on other players' pickups
			ArrayList<ArrayList<Point>> pPredictPaths = predictOthersPaths(passenger);
			for (ArrayList<Point> pPredictPath : pPredictPaths) {
				if (pPredictPath.size() < pPredictPaths.size()) {
					score += K_OTHERS_AHEAD;
					break;
				}
			}

			return score;
		}
		
		private ArrayList<ArrayList<Point>> predictOthersPaths(Passenger p) {
			ArrayList<ArrayList<Point>> predicts = new ArrayList<ArrayList<Point>>();

			if (players == null) {
				return predicts;
			}

			for (Player player : players) {
				if (player != null && !player.equals(getMe())
						&& player.getLimo().getPassenger() == null
						&& player.getPickUp() != null
						&& player.getPickUp().size() > 0
						&& player.getPickUp().get(0) == p) {
					ArrayList<Point> path = SimpleAStar.CalculatePath(getGameMap(), player.getLimo().getMapPosition(), p.getLobby().getBusStop());
					predicts.add(path);
				}
			}

			return predicts;
		}
    }
    
    private int countRoundSize() {
		int size = 0;

		MapSquare[][] squares = getGameMap().getSquares();
		for (int x = 0; x < squares.length; x++) {
			for (int y = 0; y < squares[x].length; y++) {
				if (squares[x][y].getType() == MapSquare.TYPE.ROAD) {
					size++;
				}
			}
		}

		return size;
	}
}