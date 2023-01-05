// SPDX-License-Identifier: MIT
pragma solidity ^0.8.0;

import "./Timer.sol";

/// This contract represents most simple crowdfunding campaign.
/// This contract does not protects investors from not receiving goods
/// they were promised from crowdfunding owner. This kind of contract
/// might be suitable for campaigns that does not promise anything to the
/// investors except that they will start working on some project.
/// (e.g. almost all blockchain spinoffs.)
contract Crowdfunding {

    address private owner;

    Timer private timer;

    uint256 public goal;

    uint256 public totalReceived;

    uint256 public endTimestamp;

    mapping (address => uint256) public investments;

    constructor(
        address _owner,
        Timer _timer,
        uint256 _goal,
        uint256 _endTimestamp
    ) {
        owner = (_owner == address(0) ? msg.sender : _owner);
        timer = _timer; // Not checking if this is correctly injected.
        goal = _goal;
        endTimestamp = _endTimestamp;
        totalReceived = 0.0;
    }

    function invest() public payable {
        require(timer.getTime() <= endTimestamp, "The campaign has ended");

        investments[msg.sender] += msg.value;
        totalReceived += msg.value;

    }

    function claimFunds() public { 
        require(msg.sender == owner, "Only owner can claim Funds");
        require(timer.getTime() > endTimestamp, "The campaign is still active");
        require(totalReceived >= goal, "The campaign goal has not been reached");

        payable(owner).transfer(totalReceived);
    }

    function refund() public {
        require(timer.getTime() > endTimestamp, "The campaign is still active");
        require(totalReceived < goal, "The campaign coal is reached");

        payable(msg.sender).transfer(investments[msg.sender]);
        investments[msg.sender] = 0;
    }
    
}