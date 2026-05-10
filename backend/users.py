from fastapi import APIRouter, Depends, HTTPException, status
from sqlalchemy.orm import Session
from database import get_db
from dependencies import get_current_user
import models
import meetwise_schemas as schemas



router = APIRouter(tags=["User Utilities"])

@router.patch("/availability/{avail_id}", response_model=schemas.AvailabilityResponse)
def update_availability(
    avail_id: int,
    update_data: schemas.AvailabilityUpdate,
    db: Session = Depends(get_db),
    current_user: models.User = Depends(get_current_user)
):
    """
    Partial update of a specific availability window.
    """
    avail = db.query(models.Availability).filter(
        models.Availability.availId == avail_id,
        models.Availability.userId == current_user.userId
    ).first()
    
    if not avail:
        raise HTTPException(status_code=404, detail="Availability window not found")
        
    # Apply partial updates
    for key, value in update_data.model_dump(exclude_unset=True).items():
        setattr(avail, key, value)
        
    db.commit()
    db.refresh(avail)
    return avail

@router.post("/devices/register", status_code=status.HTTP_201_CREATED)
def register_device(
    device_data: schemas.DeviceRegister,
    db: Session = Depends(get_db),
    current_user: models.User = Depends(get_current_user)
):
    """
    Register or update an FCM token for the current user.
    """
    # Check if this token is already registered for this user
    existing_device = db.query(models.UserDevice).filter(
        models.UserDevice.userId == current_user.userId,
        models.UserDevice.fcmToken == device_data.fcmToken
    ).first()
    
    if existing_device:
        # Just update the registeredAt timestamp if it already exists
        from sqlalchemy.sql import func
        existing_device.registeredAt = func.now()
    else:
        new_device = models.UserDevice(
            userId=current_user.userId,
            fcmToken=device_data.fcmToken
        )
        db.add(new_device)
        
    db.commit()
    return {"message": "Device registered successfully"}
