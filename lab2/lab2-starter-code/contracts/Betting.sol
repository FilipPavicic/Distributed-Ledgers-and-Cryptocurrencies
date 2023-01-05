// SPDX-License-Identifier: MIT
pragma solidity ^0.8.0;

import "./BoxOracle.sol";

contract Betting {

    struct Player {
        uint8 id;
        string name;
        uint totalBetAmount;
        uint currCoef; 
    }
    struct Bet {
        address bettor;
        uint amount;
        uint player_id;
        uint betCoef;
    }

    address private betMaker;
    BoxOracle public oracle;
    uint public minBetAmount;
    uint public maxBetAmount;
    uint public totalBetAmount;
    uint public thresholdAmount;

    Bet[] private bets;
    Player public player_1;
    Player public player_2;

    bool private suspended = false;
    mapping (address => uint) public balances;
    
    constructor(
        address _betMaker,
        string memory _player_1,
        string memory _player_2,
        uint _minBetAmount,
        uint _maxBetAmount,
        uint _thresholdAmount,
        BoxOracle _oracle
    ) {
        betMaker = (_betMaker == address(0) ? msg.sender : _betMaker);
        player_1 = Player(1, _player_1, 0, 200);
        player_2 = Player(2, _player_2, 0, 200);
        minBetAmount = _minBetAmount;
        maxBetAmount = _maxBetAmount;
        thresholdAmount = _thresholdAmount;
        oracle = _oracle;

        totalBetAmount = 0;
    }

    receive() external payable {}

    fallback() external payable {}

    
    
    function makeBet(uint8 _playerId) public payable {
        
        require(suspended == false, 'Betting is suspended');
        require(msg.sender != betMaker, 'Bet Maker can not bet');
        require(oracle.getWinner() == 0, 'Match finished');
        require(msg.value >= minBetAmount && msg.value <= maxBetAmount, 'Invalid bet amount');

        

        if(_playerId == player_1.id) {
            Bet memory newBet = Bet(msg.sender, msg.value, player_1.id, player_1.currCoef);
            bets.push(newBet);
            player_1.totalBetAmount += msg.value;
        } else if (_playerId == player_2.id) {
            Bet memory newBet = Bet(msg.sender, msg.value, player_2.id, player_2.currCoef);
            bets.push(newBet);
            player_2.totalBetAmount += msg.value;
        } else {
            revert("Invalid player ID");
        }

        totalBetAmount += msg.value;
        balances[msg.sender] += msg.value;

        if (totalBetAmount >= thresholdAmount) {

            if (player_1.totalBetAmount == 0 || player_2.totalBetAmount == 0) {
                suspended = true;
            } else {
                player_1.currCoef = (player_1.totalBetAmount + player_2.totalBetAmount) * 100 / player_1.totalBetAmount ;
                player_2.currCoef = (player_1.totalBetAmount + player_2.totalBetAmount)  * 100 / player_2.totalBetAmount;
            }
        }      
    }

    function claimSuspendedBets() public {
        
        require(suspended, "Betting is not suspended");

        payable(msg.sender).transfer(balances[msg.sender]);

    }

    function claimWinningBets() public {

        require(oracle.getWinner() != 0, "Match is not over");
        require(suspended == false, 'Betting is suspended');

        for (uint i = 0; i < bets.length; i++) {
            
            if (msg.sender == bets[i].bettor) {
                balances[msg.sender] -= bets[i].amount;
                if (oracle.getWinner() == bets[i].player_id) {
                    balances[msg.sender] += bets[i].amount * bets[i].betCoef / 100;  
                    //emit PrintBalance("Unutar claimWinningBets, sada vratimo duplo, ocekivano 60", balances[msg.sender]);  
                }
                bets[i].amount = 0;    
            }
        }

        payable(msg.sender).transfer(balances[msg.sender]);
    }


    function claimLosingBets() public {

        require(suspended == false, 'Betting is suspended');
        require(oracle.getWinner() != 0, "Match is not over");

        uint totalLossBetsAmount = 0;

        for (uint i = 0; i < bets.length; i++) {
            if (oracle.getWinner() != bets[i].player_id) {
                totalLossBetsAmount += bets[i].amount;
                bets[i].amount = 0;
            }
        }

        payable(betMaker).transfer(totalLossBetsAmount);
    }
}