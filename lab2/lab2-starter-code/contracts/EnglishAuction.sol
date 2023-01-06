// SPDX-License-Identifier: MIT
pragma solidity ^0.8.0;

import "./Auction.sol";

contract EnglishAuction is Auction {

    uint internal highestBid;
    uint internal initialPrice;
    uint internal biddingPeriod;
    uint internal lastBidTimestamp;
    uint internal minimumPriceIncrement;
    address currentHighestBidder;
    address internal highestBidder;

    constructor(
        address _sellerAddress,
        address _judgeAddress,
        Timer _timer,
        uint _initialPrice,
        uint _biddingPeriod,
        uint _minimumPriceIncrement
    ) Auction(_sellerAddress, _judgeAddress, _timer) {
        initialPrice = _initialPrice;
        biddingPeriod = _biddingPeriod;
        minimumPriceIncrement = _minimumPriceIncrement;

        // Start the auction at contract creation.
        lastBidTimestamp = time();
    }

    function bid() public payable {
        // Make sure that the auction has not finished yet
        require(outcome == Outcome.NOT_FINISHED, "Auction has finished, no more bids are allowed.");

        // Check if the bid was placed within the bidding period.
        require(time() < lastBidTimestamp + biddingPeriod, "Bid must be placed within the bidding period.");

        // Check if the bid is higher than the initial price.
        require(msg.value >= initialPrice, "Bid must not be lower than the initial price.");

        require(msg.value >= highestBid + minimumPriceIncrement, "Bid must be higher than the current highest bid by at least the minimum price increment.");
        // Refund the bidder if the bid is not high enough.
        if (msg.value < highestBid + minimumPriceIncrement) {
            payable(msg.sender).transfer(msg.value);
        }

        // Refund the previous highest bidder if there is one.
        if (currentHighestBidder != address(0)) {
            payable(currentHighestBidder).transfer(highestBid);
        }

        // Update the highest bid and highest bidder.
        highestBid = msg.value;
        currentHighestBidder = msg.sender;

        // Update the timestamp of the last bid.
        lastBidTimestamp = time();
    }

    function getHighestBidder() override public returns (address) {
        if(time() >= lastBidTimestamp + biddingPeriod) {
            highestBidder = currentHighestBidder;
        }
        return address(highestBidder);
    }

    function enableRefunds() public {
        // Check if the bidding period has passed.
        require(time() >= lastBidTimestamp + biddingPeriod, "Refund not possible because the bidding period has not passed yet.");
        highestBidder = currentHighestBidder;
        require(highestBidder == address(0), "Refund not possible because auction was successful.");
        outcome = Outcome.NOT_SUCCESSFUL;
    }

}